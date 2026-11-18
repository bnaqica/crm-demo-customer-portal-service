package com.bnaqica.crmdemocustomerportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.Tier;
import com.bnaqica.crmdemocustomerportalservice.dto.request.CorrectionRequest;
import com.bnaqica.crmdemocustomerportalservice.dto.request.SubmissionRequest;
import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import com.bnaqica.crmdemocustomerportalservice.repository.DocumentRepository;
import com.bnaqica.crmdemocustomerportalservice.repository.OutboxEventRepository;
import com.bnaqica.crmdemocustomerportalservice.repository.SubmissionRepository;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers.KeycloakContainer;
import com.bnaqica.crmdemocustomerportalservice.support.PortalApi;
import com.bnaqica.crmdemocustomerportalservice.support.TestKafka;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.JsonNode;

/** submission-events spec: SubmissionReceived saved with the submission and published after commit. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestContainers.class)
class SubmissionEventsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeycloakContainer keycloak;

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private DocumentRepository documents;

    @Autowired
    private OutboxEventRepository outbox;

    @Autowired
    private SubmissionRepository submissions;

    @Autowired
    private PortalSubmissionService service;

    private PortalApi user;

    @BeforeEach
    void setUp() throws Exception {
        user = new PortalApi(mockMvc, keycloak, documents, "external-standard");
    }

    private List<JsonNode> received() {
        return TestKafka.readMatching(
                        kafka,
                        "portal-intake",
                        record -> user.sub().toString().equals(record.key())
                                && "SubmissionReceived".equals(TestKafka.header(record, "type")),
                        Duration.ofSeconds(60))
                .stream()
                .map(ConsumerRecord::value)
                .map(PortalApi.JSON::readTree)
                .toList();
    }

    @Test
    void eventListsDocumentReferencesAndNoFileContents() throws Exception {
        UUID first = user.document(DocumentStatus.CLEAN);
        UUID second = user.document(DocumentStatus.CLEAN);

        String id = PortalApi.field(
                user.post(
                        "/submissions",
                        Map.of(
                                "type",
                                "CLAIM",
                                "region",
                                "EU",
                                "data",
                                Map.of("amount", 120),
                                "documentIds",
                                List.of(first, second))),
                "submissionId");

        List<JsonNode> events = received();
        assertThat(events).hasSize(1);
        JsonNode event = events.getFirst();
        assertThat(event.path("eventId").asString()).isNotBlank();
        assertThat(event.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(event.path("occurredAt").asString()).isNotBlank();
        assertThat(event.path("portalUserId").asString()).isEqualTo(user.sub().toString());
        assertThat(event.path("email").asString()).endsWith("@acme.example");
        assertThat(event.path("submissionId").asString()).isEqualTo(id);
        assertThat(event.path("version").asInt()).isEqualTo(1);
        assertThat(event.path("type").asString()).isEqualTo("CLAIM");
        assertThat(event.path("region").asString()).isEqualTo("EU");
        assertThat(event.path("documents")).hasSize(2);
        JsonNode document = event.path("documents").get(0);
        assertThat(document.propertyNames())
                .containsExactlyInAnyOrder("documentId", "storageKey", "contentType", "size", "sha256");
        assertThat(event.propertyNames())
                .containsExactlyInAnyOrder(
                        "eventId",
                        "schemaVersion",
                        "occurredAt",
                        "portalUserId",
                        "email",
                        "submissionId",
                        "version",
                        "type",
                        "region",
                        "documents");
    }

    @Test
    void correctionPublishesSubmissionReceivedWithVersion2() throws Exception {
        UUID document = user.document(DocumentStatus.CLEAN);
        String id = PortalApi.field(
                user.post(
                        "/submissions",
                        Map.of("type", "CLAIM", "region", "EU", "data", Map.of(), "documentIds", List.of(document))),
                "submissionId");

        user.post("/submissions/" + id + "/versions", Map.of("data", Map.of("fixed", true), "documentIds", List.of()));

        assertThat(received())
                .extracting(event -> event.path("version").asInt())
                .containsExactlyInAnyOrder(1, 2);
    }

    /** A double-clicked correction: both requests succeed, one after the other, as versions 2 and 3. */
    @Test
    void concurrentCorrectionsGetConsecutiveVersions() throws Exception {
        var principal = new PortalPrincipal(user.sub(), "x@acme.example", "X", Tier.STANDARD, Instant.now());
        UUID id = service.create(principal, new SubmissionRequest("CLAIM", "EU", Map.of(), List.of()))
                .submissionId();
        var correction = new CorrectionRequest(Map.of("fixed", true), List.of());

        try (var pool = Executors.newFixedThreadPool(2)) {
            var first =
                    pool.submit(() -> service.correct(principal, id, correction).version());
            var second =
                    pool.submit(() -> service.correct(principal, id, correction).version());

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(2, 3);
        }
    }

    @Test
    void failedSaveStoresAndPublishesNoEvent() {
        var principal = new PortalPrincipal(user.sub(), "x@acme.example", "X", Tier.STANDARD, Instant.now());
        // Longer than the column allows; only the database rejects it, so the failure comes when the change commits
        var tooLong = new SubmissionRequest("C".repeat(60), "EU", Map.of(), List.of());

        assertThatThrownBy(() -> service.create(principal, tooLong)).isInstanceOf(RuntimeException.class);

        assertThat(outbox.findAll())
                .noneMatch(event -> event.getMessageKey().equals(user.sub().toString())
                        && "SubmissionReceived".equals(event.getEventType()));
        assertThat(submissions.findAll())
                .noneMatch(submission -> submission.getOwnerSub().equals(user.sub()));
    }
}

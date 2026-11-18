package com.bnaqica.crmdemocustomerportalservice.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import com.bnaqica.crmdemocustomerportalservice.repository.DocumentRepository;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers.KeycloakContainer;
import com.bnaqica.crmdemocustomerportalservice.support.PortalApi;
import com.bnaqica.crmdemocustomerportalservice.support.TestKafka;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;

/** submission-events "Status updates applied" and portal-submissions "Status shown to owner", over real Kafka. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestContainers.class)
class SubmissionStatusTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeycloakContainer keycloak;

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private DocumentRepository documents;

    @Autowired
    private KafkaTemplate<String, String> template;

    private PortalApi user;
    private String submissionId;

    @BeforeEach
    void setUp() throws Exception {
        user = new PortalApi(mockMvc, keycloak, documents, "external-standard");
        submissionId = PortalApi.field(
                user.post(
                        "/submissions",
                        Map.of(
                                "type",
                                "CLAIM",
                                "region",
                                "EU",
                                "data",
                                Map.of(),
                                "documentIds",
                                List.of(user.document(DocumentStatus.CLEAN)))),
                "submissionId");
    }

    private String event(String eventId, String submission, String status, String reason) {
        var event = new LinkedHashMap<String, Object>(Map.of(
                "eventId",
                eventId,
                "schemaVersion",
                1,
                "occurredAt",
                "2026-11-12T10:00:00Z",
                "submissionId",
                submission,
                "version",
                1,
                "portalUserId",
                user.sub().toString(),
                "status",
                status));
        if (reason != null) {
            event.put("reason", reason);
        }
        return PortalApi.JSON.writeValueAsString(event);
    }

    private void send(String type, String payload) throws Exception {
        var record = new ProducerRecord<>("submission-status", submissionId, payload);
        record.headers().add("type", type.getBytes(StandardCharsets.UTF_8));
        template.send(record).get();
    }

    private void awaitStatus(String status) {
        await().atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> user.get("/submissions/" + submissionId)
                        .andExpect(jsonPath("$.status").value(status)));
    }

    @Test
    void acceptedEventShowsAccepted() throws Exception {
        send("SubmissionAccepted", event(UUID.randomUUID().toString(), submissionId, "ACCEPTED", null));

        awaitStatus("ACCEPTED");
    }

    @Test
    void rejectedSubmissionShowsStatusAndReason() throws Exception {
        send("SubmissionRejected", event(UUID.randomUUID().toString(), submissionId, "REJECTED", "Missing invoice"));

        awaitStatus("REJECTED");
        user.get("/submissions/" + submissionId)
                .andExpect(jsonPath("$.rejectionReason").value("Missing invoice"));
    }

    @Test
    void unknownSubmissionGoesToTheDlq() throws Exception {
        String eventId = UUID.randomUUID().toString();

        send("SubmissionAccepted", event(eventId, UUID.randomUUID().toString(), "ACCEPTED", null));

        var dead = TestKafka.readMatching(
                kafka, "submission-status-dlq", record -> record.value().contains(eventId), Duration.ofSeconds(60));
        assertThat(dead).hasSize(1);
    }
}

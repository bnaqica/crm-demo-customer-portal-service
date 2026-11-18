package com.bnaqica.crmdemocustomerportalservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocustomerportalservice.repository.PortalUserRepository;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers.KeycloakContainer;
import com.bnaqica.crmdemocustomerportalservice.support.KeycloakTestAdmin;
import com.bnaqica.crmdemocustomerportalservice.support.TestKafka;
import com.bnaqica.crmdemocustomerportalservice.support.TestTokens;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** portal-access spec, "Portal user recorded on first call", with a real token, database and Kafka. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestContainers.class)
class PortalUserProvisioningTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeycloakContainer keycloak;

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private PortalUserRepository users;

    /** No controller here: a 404 means the request got past security. */
    private static final String PROBE = "/no-such-endpoint";

    @Test
    void firstCallRecordsTheUserAndPublishesOneRegistrationAuditEvent() throws Exception {
        var admin = new KeycloakTestAdmin(keycloak);
        String email = admin.createPortalUser("external-standard", true);
        UUID sub = UUID.fromString(admin.idOf(email));
        String token = new TestTokens(keycloak).portalToken(email);

        mockMvc.perform(get(PROBE).header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
        mockMvc.perform(get(PROBE).header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());

        assertThat(users.findById(sub))
                .hasValueSatisfying(user -> assertThat(user.getEmail()).isEqualTo(email));
        var registrations = TestKafka.readMatching(
                        kafka, "audit-events", record -> sub.toString().equals(record.key()), Duration.ofSeconds(60))
                .stream()
                .map(record -> JSON.readTree(record.value()))
                .toList();
        assertThat(registrations).hasSize(1);
        JsonNode event = registrations.getFirst();
        assertThat(event.path("action").asString()).isEqualTo("PORTAL_USER_REGISTERED");
        assertThat(event.path("actorId").asString()).isEqualTo(sub.toString());
        assertThat(event.path("outcome").asString()).isEqualTo("SUCCESS");
        assertThat(event.path("realm").asString()).isEqualTo("portal");
    }

    @Test
    void deniedCallDoesNotRecordTheUser() throws Exception {
        var admin = new KeycloakTestAdmin(keycloak);
        String email = admin.createPortalUser("external-starter", true);
        UUID sub = UUID.fromString(admin.idOf(email));
        String token = new TestTokens(keycloak).portalToken(email);

        mockMvc.perform(get(PROBE).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());

        assertThat(users.findById(sub)).isEmpty();
    }
}

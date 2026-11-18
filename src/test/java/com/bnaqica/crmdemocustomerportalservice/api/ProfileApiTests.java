package com.bnaqica.crmdemocustomerportalservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocustomerportalservice.repository.PortalUserRepository;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers.KeycloakContainer;
import com.bnaqica.crmdemocustomerportalservice.support.KeycloakTestAdmin;
import com.bnaqica.crmdemocustomerportalservice.support.TestKafka;
import com.bnaqica.crmdemocustomerportalservice.support.TestTokens;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** portal-profile spec with real Keycloak tokens, database and Kafka. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestContainers.class)
class ProfileApiTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeycloakContainer keycloak;

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private PortalUserRepository users;

    private KeycloakTestAdmin admin;
    private TestTokens tokens;

    @BeforeEach
    void setUp() {
        admin = new KeycloakTestAdmin(keycloak);
        tokens = new TestTokens(keycloak);
    }

    @Test
    void starterProfileListsOnlyStarterFeatures() throws Exception {
        String email = admin.createPortalUser("external-starter", true);
        String sub = admin.idOf(email);

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + tokens.portalToken(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value(sub))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.tier").value("STARTER"))
                .andExpect(jsonPath("$.features").value(org.hamcrest.Matchers.contains("PROFILE", "DOCUMENTS")));
    }

    @Test
    void standardProfileHasStandardTier() throws Exception {
        String email = admin.createPortalUser("external-standard", true);

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + tokens.portalToken(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("STANDARD"));
    }

    @Test
    void emailChangedInKeycloakIsStoredAndPublishedOnce() throws Exception {
        String oldEmail = admin.createPortalUser("external-standard", true);
        UUID sub = UUID.fromString(admin.idOf(oldEmail));
        mockMvc.perform(get("/me").header("Authorization", "Bearer " + tokens.portalToken(oldEmail)))
                .andExpect(status().isOk());

        String newEmail = admin.changeEmail(oldEmail, "new-" + UUID.randomUUID() + "@acme.example");
        String newToken = tokens.portalToken(newEmail);
        mockMvc.perform(get("/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail));
        mockMvc.perform(get("/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());

        assertThat(users.findById(sub).orElseThrow().getEmail()).isEqualTo(newEmail);
        var published = TestKafka.readMatching(
                kafka, "portal-intake", record -> sub.toString().equals(record.key()), Duration.ofSeconds(60));
        assertThat(published).hasSize(1);
        assertThat(TestKafka.header(published.getFirst(), "type")).isEqualTo("PortalUserEmailChanged");
        JsonNode event = JSON.readTree(published.getFirst().value());
        assertThat(event.path("portalUserId").asString()).isEqualTo(sub.toString());
        assertThat(event.path("email").asString()).isEqualTo(newEmail);
        assertThat(event.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(event.path("eventId").asString()).isNotBlank();
        assertThat(event.path("occurredAt").asString()).isNotBlank();
    }
}

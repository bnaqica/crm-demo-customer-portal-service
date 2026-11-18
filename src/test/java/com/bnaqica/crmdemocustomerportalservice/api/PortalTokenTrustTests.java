package com.bnaqica.crmdemocustomerportalservice.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers.KeycloakContainer;
import com.bnaqica.crmdemocustomerportalservice.support.KeycloakTestAdmin;
import com.bnaqica.crmdemocustomerportalservice.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** portal-access spec with real Keycloak tokens from all three realms. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestContainers.class)
class PortalTokenTrustTests {

    /** No controller here: a 404 means the request got past security. */
    private static final String PROBE = "/no-such-endpoint";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeycloakContainer keycloak;

    private TestTokens tokens;
    private KeycloakTestAdmin keycloakAdmin;

    @BeforeEach
    void setUp() {
        tokens = new TestTokens(keycloak);
        keycloakAdmin = new KeycloakTestAdmin(keycloak);
    }

    @Test
    void crmInternalTokenIsRejected() throws Exception {
        String token = tokens.userToken("crm-internal", "test-employee");

        mockMvc.perform(get(PROBE).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void crmAdminTokenIsRejected() throws Exception {
        String token = tokens.userToken("crm-admin", "test-admin");

        mockMvc.perform(get(PROBE).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void portalTokenIsAccepted() throws Exception {
        String token = tokens.portalToken(TestTokens.STANDARD_USER);

        mockMvc.perform(get(PROBE).header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
    }

    /** Real role mapping end to end: the probe isn't a starter feature, so a starter is forbidden. */
    @Test
    void starterTokenOnStandardPathIsForbidden() throws Exception {
        String token = tokens.portalToken(keycloakAdmin.createPortalUser("external-starter", true));

        mockMvc.perform(get(PROBE).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }
}

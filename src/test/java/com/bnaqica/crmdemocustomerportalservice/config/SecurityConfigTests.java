package com.bnaqica.crmdemocustomerportalservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocustomerportalservice.service.PortalUserSyncService;
import jakarta.servlet.DispatcherType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * portal-access spec: starter and standard features, and the verified-email rule. Uses two test-only endpoints, one
 * marked {@link StarterFeature} and one not. A 404 means the request got past security.
 */
@WebMvcTest(useDefaultFilters = false) // only the test controller, so this keeps working as the service grows
@Import({SecurityConfig.class, SecurityConfigTests.FeatureController.class})
class SecurityConfigTests {

    private final MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private PortalUserSyncService syncService;

    SecurityConfigTests(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @RestController
    static class FeatureController {

        @StarterFeature
        @GetMapping("/test/starter")
        String starter() {
            return "starter";
        }

        @GetMapping("/test/standard")
        String standard() {
            return "standard";
        }
    }

    private static JwtRequestPostProcessor user(String role, boolean emailVerified) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                        .claim("email", "user@acme.example")
                        .claim("email_verified", emailVerified))
                .authorities(new SimpleGrantedAuthority(role));
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/test/starter")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthCheckIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
    }

    @Test
    void starterUsesStarterFeature() throws Exception {
        mockMvc.perform(get("/test/starter").with(user(PortalRoles.STARTER, true)))
                .andExpect(status().isOk());
    }

    @Test
    void standardUsesStarterFeature() throws Exception {
        mockMvc.perform(get("/test/starter").with(user(PortalRoles.STANDARD, true)))
                .andExpect(status().isOk());
    }

    @Test
    void standardUsesStandardFeature() throws Exception {
        mockMvc.perform(get("/test/standard").with(user(PortalRoles.STANDARD, true)))
                .andExpect(status().isOk());
    }

    @Test
    void starterUsesStandardFeatureIsForbidden() throws Exception {
        mockMvc.perform(get("/test/standard").with(user(PortalRoles.STARTER, true)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenWithoutPortalRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/test/starter").with(user("ROLE_OFFLINE_ACCESS", true)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unverifiedEmailIsForbidden() throws Exception {
        mockMvc.perform(get("/test/starter").with(user(PortalRoles.STANDARD, false)))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingEmailVerifiedClaimIsForbidden() throws Exception {
        mockMvc.perform(get("/test/starter")
                        .with(jwt().jwt(token -> token.claim("email", "user@acme.example"))
                                .authorities(new SimpleGrantedAuthority(PortalRoles.STANDARD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void portalUiOriginMayCallTheApi() throws Exception {
        mockMvc.perform(options("/test/starter")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"));
    }

    @Test
    void otherOriginIsRefused() throws Exception {
        mockMvc.perform(options("/test/starter")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    /**
     * An error inside the chain is re-dispatched to {@code /error}; that dispatch mustn't be denied, or a starter's
     * 500 would turn into 403.
     */
    @Test
    void errorDispatchIsNotDenied() throws Exception {
        mockMvc.perform(get("/error").with(user(PortalRoles.STARTER, true)).with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    return request;
                }))
                .andExpect(
                        result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void tokenWithoutEmailIsForbidden() throws Exception {
        mockMvc.perform(get("/test/starter")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                                        .claim("email_verified", true)
                                        .claims(claims -> claims.remove("email")))
                                .authorities(new SimpleGrantedAuthority(PortalRoles.STANDARD))))
                .andExpect(status().isForbidden());
    }

    /** Deny by default: a path with no endpoint needs the standard role like any other. */
    @Test
    void starterOnUnknownPathIsForbidden() throws Exception {
        mockMvc.perform(get("/test/unknown").with(user(PortalRoles.STARTER, true)))
                .andExpect(status().isForbidden());
    }
}

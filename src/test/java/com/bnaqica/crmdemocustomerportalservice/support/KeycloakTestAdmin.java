package com.bnaqica.crmdemocustomerportalservice.support;

import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers.KeycloakContainer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Test-only setup through Keycloak's master admin, for portal users the generated test realm doesn't have (starters,
 * unverified users, users whose email changes). Each call creates a fresh user, so tests don't share state.
 */
public class KeycloakTestAdmin {

    private static final String REALM = "portal";

    private final KeycloakContainer keycloak;
    private final RestClient rest = RestClient.create();

    public KeycloakTestAdmin(KeycloakContainer keycloak) {
        this.keycloak = keycloak;
    }

    /** A sign-in-ready portal user with a random {@code @acme.example} email; returns the email (the username). */
    public String createPortalUser(String role, boolean emailVerified) {
        String email = "user-" + UUID.randomUUID() + "@acme.example";
        var credential = Map.of("type", "password", "value", TestTokens.PASSWORD, "temporary", false);
        var user = Map.of(
                "username",
                email,
                "email",
                email,
                "firstName",
                "Test",
                "lastName",
                "User",
                "enabled",
                true,
                "emailVerified",
                emailVerified,
                "credentials",
                List.of(credential));
        rest.post()
                .uri(users())
                .header("Authorization", "Bearer " + masterToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(user)
                .retrieve()
                .toBodilessEntity();
        if (role != null) {
            Map<?, ?> roleRepresentation = rest.get()
                    .uri(keycloak.baseUrl() + "/admin/realms/" + REALM + "/roles/" + role)
                    .header("Authorization", "Bearer " + masterToken())
                    .retrieve()
                    .body(Map.class);
            rest.post()
                    .uri(users() + "/" + idOf(email) + "/role-mappings/realm")
                    .header("Authorization", "Bearer " + masterToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(roleRepresentation))
                    .retrieve()
                    .toBodilessEntity();
        }
        return email;
    }

    /**
     * Changes a user's email as Keycloak's account console would after verification. The registration email is
     * the username in the portal realm, so the username follows it; returns the new email.
     */
    public String changeEmail(String currentEmail, String newEmail) {
        rest.put()
                .uri(users() + "/" + idOf(currentEmail))
                .header("Authorization", "Bearer " + masterToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", newEmail, "username", newEmail, "emailVerified", true))
                .retrieve()
                .toBodilessEntity();
        return newEmail;
    }

    @SuppressWarnings("unchecked")
    public String idOf(String email) {
        List<Map<String, Object>> found = rest.get()
                .uri(users() + "?exact=true&email=" + email)
                .header("Authorization", "Bearer " + masterToken())
                .retrieve()
                .body(List.class);
        return (String) found.getFirst().get("id");
    }

    private String users() {
        return keycloak.baseUrl() + "/admin/realms/" + REALM + "/users";
    }

    private String masterToken() {
        Map<?, ?> response = rest.post()
                .uri(keycloak.tokenUri("master"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=password&client_id=admin-cli&username=admin&password=admin")
                .retrieve()
                .body(Map.class);
        return (String) response.get("access_token");
    }
}

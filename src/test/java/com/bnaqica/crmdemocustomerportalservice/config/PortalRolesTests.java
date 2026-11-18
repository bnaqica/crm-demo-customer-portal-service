package com.bnaqica.crmdemocustomerportalservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class PortalRolesTests {

    private static Jwt token(Map<String, Object> claims) {
        var builder = Jwt.withTokenValue("t").header("alg", "none").subject("s").issuedAt(Instant.now());
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    void portalRealmRolesBecomeRoleAuthorities() {
        Jwt jwt = token(Map.of("realm_access", Map.of("roles", List.of("external-starter", "external-standard"))));

        var authorities = PortalRoles.authorities(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_EXTERNAL_STARTER", "ROLE_EXTERNAL_STANDARD");
    }

    @Test
    void tokenWithoutRealmRolesHasNoAuthorities() {
        assertThat(PortalRoles.authorities(token(Map.of("scope", "openid")))).isEmpty();
    }
}

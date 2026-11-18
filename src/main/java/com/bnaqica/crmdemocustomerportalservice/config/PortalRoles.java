package com.bnaqica.crmdemocustomerportalservice.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/** The portal realm's roles, and how Keycloak realm roles ({@code realm_access.roles}) become authorities. */
public final class PortalRoles {

    public static final String STARTER = "ROLE_EXTERNAL_STARTER";
    public static final String STANDARD = "ROLE_EXTERNAL_STANDARD";

    private PortalRoles() {}

    /** Each realm role becomes {@code ROLE_<NAME>}, upper case with hyphens as underscores. */
    public static Collection<GrantedAuthority> authorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(
                        "ROLE_" + role.toString().toUpperCase().replace('-', '_')))
                .toList();
    }

    static JwtAuthenticationConverter converter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(PortalRoles::authorities);
        return converter;
    }
}

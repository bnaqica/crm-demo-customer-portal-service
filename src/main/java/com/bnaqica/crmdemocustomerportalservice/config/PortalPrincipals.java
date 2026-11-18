package com.bnaqica.crmdemocustomerportalservice.config;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.Tier;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Builds the {@link PortalPrincipal} from a portal token. */
public final class PortalPrincipals {

    private PortalPrincipals() {}

    public static PortalPrincipal from(Jwt jwt) {
        boolean standard = PortalRoles.authorities(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PortalRoles.STANDARD::equals);
        return new PortalPrincipal(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"),
                standard ? Tier.STANDARD : Tier.STARTER,
                jwt.getIssuedAt());
    }
}

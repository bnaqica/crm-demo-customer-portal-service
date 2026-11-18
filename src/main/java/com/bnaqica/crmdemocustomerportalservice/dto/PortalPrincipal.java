package com.bnaqica.crmdemocustomerportalservice.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * The calling portal user, taken from their verified token and never from request parameters.
 *
 * @param sub the token's {@code sub}; owner of everything the user creates
 * @param email the token's verified email
 * @param name the token's display name, or null
 * @param tokenIssuedAt the token's {@code iat}
 */
public record PortalPrincipal(UUID sub, String email, String name, Tier tier, Instant tokenIssuedAt) {}

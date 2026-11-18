package com.bnaqica.crmdemocustomerportalservice.dto.event;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * {@code PortalUserEmailChanged} v1, published to {@code portal-intake} with the {@code type} header
 * {@code PortalUserEmailChanged}, keyed by {@code portalUserId}. Contract shared with core-service.
 */
@Builder
public record PortalUserEmailChanged(
        UUID eventId, int schemaVersion, Instant occurredAt, UUID portalUserId, String email) {}

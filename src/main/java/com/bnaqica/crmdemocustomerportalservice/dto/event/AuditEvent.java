package com.bnaqica.crmdemocustomerportalservice.dto.event;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * Published to {@code audit-events}. Follows the common audit schema shared by Keycloak, UMS, CRM and portal records.
 *
 * @param after the target's state after the change, or null
 */
@Builder
public record AuditEvent(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        String source,
        String realm,
        String actorId,
        String actorType,
        AuditAction action,
        String targetType,
        String targetId,
        String outcome,
        Object after,
        String ipAddress,
        String correlationId) {}

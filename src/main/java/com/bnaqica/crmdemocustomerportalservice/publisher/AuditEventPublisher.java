package com.bnaqica.crmdemocustomerportalservice.publisher;

import com.bnaqica.crmdemocustomerportalservice.config.PortalProperties;
import com.bnaqica.crmdemocustomerportalservice.dto.event.AuditAction;
import com.bnaqica.crmdemocustomerportalservice.dto.event.AuditEvent;
import com.bnaqica.crmdemocustomerportalservice.service.OutboxService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes audit events to the outbox. Call inside the transaction that makes the change, so the event is published
 * only if the change commits.
 */
@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

    static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final OutboxService outbox;
    private final JsonMapper jsonMapper;
    private final PortalProperties properties;
    private final Clock clock;

    /** The user acts on themself: they are both the actor and the target. */
    public void portalUserRegistered(UUID sub, String email) {
        HttpServletRequest request = currentRequest();
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .schemaVersion(1)
                .occurredAt(Instant.now(clock))
                .source("CUSTOMER_PORTAL")
                .realm("portal")
                .actorId(sub.toString())
                .actorType("PORTAL_USER")
                .action(AuditAction.PORTAL_USER_REGISTERED)
                .targetType("PORTAL_USER")
                .targetId(sub.toString())
                .outcome("SUCCESS")
                .after(Map.of("email", email))
                .ipAddress(request == null ? null : request.getRemoteAddr())
                .correlationId(correlationId(request))
                .build();
        outbox.add(properties.topics().auditEvents(), sub.toString(), null, jsonMapper.writeValueAsString(event));
    }

    private static String correlationId(HttpServletRequest request) {
        String header = request == null ? null : request.getHeader(CORRELATION_HEADER);
        return header == null || header.isBlank() ? UUID.randomUUID().toString() : header;
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }
}

package com.bnaqica.crmdemocustomerportalservice.publisher;

import com.bnaqica.crmdemocustomerportalservice.config.PortalProperties;
import com.bnaqica.crmdemocustomerportalservice.dto.event.PortalUserEmailChanged;
import com.bnaqica.crmdemocustomerportalservice.dto.event.SubmissionReceived;
import com.bnaqica.crmdemocustomerportalservice.entity.Document;
import com.bnaqica.crmdemocustomerportalservice.entity.Submission;
import com.bnaqica.crmdemocustomerportalservice.entity.SubmissionVersion;
import com.bnaqica.crmdemocustomerportalservice.service.OutboxService;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes events for the CRM to the outbox for {@code portal-intake}, keyed by the portal user, so one user's events
 * stay in order. Call inside the transaction that makes the change.
 */
@Component
@RequiredArgsConstructor
public class PortalIntakePublisher {

    static final int SCHEMA_VERSION = 1;

    private final OutboxService outbox;
    private final JsonMapper jsonMapper;
    private final PortalProperties properties;
    private final Clock clock;

    public void emailChanged(UUID portalUserId, String email) {
        publish(
                portalUserId,
                "PortalUserEmailChanged",
                PortalUserEmailChanged.builder()
                        .eventId(UUID.randomUUID())
                        .schemaVersion(SCHEMA_VERSION)
                        .occurredAt(Instant.now(clock))
                        .portalUserId(portalUserId)
                        .email(email)
                        .build());
    }

    /** For a new submission ({@code version} 1) or a correction. */
    public void submissionReceived(String email, Submission submission, SubmissionVersion version) {
        publish(
                submission.getOwnerSub(),
                "SubmissionReceived",
                SubmissionReceived.builder()
                        .eventId(UUID.randomUUID())
                        .schemaVersion(SCHEMA_VERSION)
                        .occurredAt(Instant.now(clock))
                        .portalUserId(submission.getOwnerSub())
                        .email(email)
                        .submissionId(submission.getId())
                        .version(version.getVersion())
                        .type(submission.getType())
                        .region(submission.getRegion())
                        .documents(version.getDocuments().stream()
                                .sorted(Comparator.comparing(Document::getId))
                                .map(document -> SubmissionReceived.DocumentReference.builder()
                                        .documentId(document.getId())
                                        .storageKey(document.getStorageKey())
                                        .contentType(document.getContentType())
                                        .size(document.getSizeBytes())
                                        .sha256(document.getSha256())
                                        .build())
                                .toList())
                        .build());
    }

    private void publish(UUID portalUserId, String type, Object event) {
        outbox.add(
                properties.topics().portalIntake(),
                portalUserId.toString(),
                type,
                jsonMapper.writeValueAsString(event));
    }
}

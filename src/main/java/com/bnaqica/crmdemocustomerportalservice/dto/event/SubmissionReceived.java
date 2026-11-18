package com.bnaqica.crmdemocustomerportalservice.dto.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/**
 * {@code SubmissionReceived} v1, published to {@code portal-intake} with the {@code type} header
 * {@code SubmissionReceived}, keyed by {@code portalUserId}, for each new submission and each correction. Contract
 * shared with core-service. Carries document references only, never file contents.
 */
@Builder
public record SubmissionReceived(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        UUID portalUserId,
        String email,
        UUID submissionId,
        int version,
        String type,
        String region,
        List<DocumentReference> documents) {

    @Builder
    public record DocumentReference(UUID documentId, String storageKey, String contentType, long size, String sha256) {}
}

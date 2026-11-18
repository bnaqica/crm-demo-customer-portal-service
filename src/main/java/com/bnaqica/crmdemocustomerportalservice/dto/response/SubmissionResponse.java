package com.bnaqica.crmdemocustomerportalservice.dto.response;

import com.bnaqica.crmdemocustomerportalservice.entity.SubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/**
 * A submission as its owner sees it: status and rejection reason from the CRM, and every version they submitted.
 *
 * @param version the current version
 * @param versions oldest first; empty in lists
 */
@Builder
public record SubmissionResponse(
        UUID submissionId,
        String type,
        String region,
        SubmissionStatus status,
        String rejectionReason,
        int version,
        Instant createdAt,
        Instant updatedAt,
        List<SubmissionVersionResponse> versions) {}

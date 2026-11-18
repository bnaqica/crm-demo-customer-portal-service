package com.bnaqica.crmdemocustomerportalservice.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SubmissionVersionResponse(
        int version, Map<String, Object> data, List<UUID> documentIds, Instant createdAt) {}

package com.bnaqica.crmdemocustomerportalservice.dto;

import lombok.Builder;

@Builder
public record ApiAuditEntry(
        String apiPath,
        String action,
        String requestPayload,
        String performedBy,
        int responseStatus,
        boolean success) {}

package com.bnaqica.crmdemocustomerportalservice.dto.response;

import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DocumentResponse(
        UUID documentId, String fileName, String contentType, long size, DocumentStatus status, Instant createdAt) {}

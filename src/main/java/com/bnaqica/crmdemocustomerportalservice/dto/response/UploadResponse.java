package com.bnaqica.crmdemocustomerportalservice.dto.response;

import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

/**
 * Where to upload the file: send a {@code PUT} to {@code uploadUrl} with {@code uploadHeaders} before
 * {@code expiresAt}. The document is then scanned; poll {@code GET /documents/{documentId}} for its status.
 */
@Builder
public record UploadResponse(
        UUID documentId,
        DocumentStatus status,
        String uploadUrl,
        Map<String, String> uploadHeaders,
        Instant expiresAt) {}

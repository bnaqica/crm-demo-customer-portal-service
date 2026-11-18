package com.bnaqica.crmdemocustomerportalservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * A request to upload one file.
 *
 * @param size the file's exact size in bytes; the upload URL only accepts that many
 */
public record UploadRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Size(max = 100) String contentType,
        @Positive long size) {}

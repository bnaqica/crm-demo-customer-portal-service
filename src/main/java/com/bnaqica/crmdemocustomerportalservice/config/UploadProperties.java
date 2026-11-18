package com.bnaqica.crmdemocustomerportalservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Set;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Settings under {@code portal.uploads}.
 *
 * @param allowedContentTypes the only file types users may upload
 * @param maxFileSize the largest single file
 * @param urlTtl how long an upload URL works; at most 10 minutes
 * @param scanGrace how long after its upload URL expires a PENDING document still counts towards the quota
 * @param quota how much each tier may store, counting files waiting for their scan
 */
@Validated
@ConfigurationProperties("portal.uploads")
public record UploadProperties(
        @DefaultValue({"application/pdf", "image/png", "image/jpeg"}) @NotEmpty
        Set<String> allowedContentTypes,

        @DefaultValue("10MB") @NotNull DataSize maxFileSize,

        @DefaultValue("10m") @DurationMin(seconds = 30) @DurationMax(minutes = 10)
        Duration urlTtl,

        @DefaultValue("1h") @NotNull Duration scanGrace,

        @DefaultValue @Valid Quota quota) {

    public record Quota(
            @DefaultValue("20MB") @NotNull DataSize starter,
            @DefaultValue("200MB") @NotNull DataSize standard) {}
}

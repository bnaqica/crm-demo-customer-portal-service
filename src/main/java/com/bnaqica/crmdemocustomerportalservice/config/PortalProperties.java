package com.bnaqica.crmdemocustomerportalservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings under {@code portal.*}.
 *
 * @param topics Kafka topic names
 */
@Validated
@ConfigurationProperties("portal")
public record PortalProperties(@NotNull @Valid Topics topics) {

    /**
     * @param portalIntake submissions and profile changes for the CRM
     * @param auditEvents the shared audit stream
     * @param submissionStatus the CRM's decisions on submissions; failures go to {@code <topic>-dlq}
     */
    public record Topics(
            @NotBlank String portalIntake,
            @NotBlank String auditEvents,
            @NotBlank String submissionStatus) {}
}

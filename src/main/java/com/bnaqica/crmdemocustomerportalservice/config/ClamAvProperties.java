package com.bnaqica.crmdemocustomerportalservice.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Settings under {@code portal.clamav}: where ClamAV's {@code clamd} listens. */
@Validated
@ConfigurationProperties("portal.clamav")
public record ClamAvProperties(
        @NotBlank String host,
        @DefaultValue("3310") int port,
        @DefaultValue("60s") Duration timeout) {}

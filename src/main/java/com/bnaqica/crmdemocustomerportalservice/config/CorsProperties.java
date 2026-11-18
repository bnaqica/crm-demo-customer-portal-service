package com.bnaqica.crmdemocustomerportalservice.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** @param allowedOrigins browser origins that may call the API, such as the Portal UI */
@Validated
@ConfigurationProperties("portal.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {}

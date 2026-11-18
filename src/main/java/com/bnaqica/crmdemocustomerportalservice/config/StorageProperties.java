package com.bnaqica.crmdemocustomerportalservice.config;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings under {@code portal.storage}: the private document bucket and its upload notifications.
 *
 * @param endpoint the S3 and SQS endpoint, for LocalStack; null for AWS itself
 * @param accessKey static credentials, for LocalStack; null to use the AWS default credentials chain
 * @param uploadQueueUrl the SQS queue the bucket sends object-created notifications to
 */
@Validated
@ConfigurationProperties("portal.storage")
public record StorageProperties(
        @NotBlank String bucket,
        @NotBlank String region,
        URI endpoint,
        String accessKey,
        String secretKey,
        @NotBlank String uploadQueueUrl) {}

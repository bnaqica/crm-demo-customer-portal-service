package com.bnaqica.crmdemocustomerportalservice.dto;

import java.time.Instant;
import java.util.Map;

/** A pre-signed upload URL and the headers that must be sent with it. */
public record PresignedUpload(String url, Map<String, String> headers, Instant expiresAt) {}

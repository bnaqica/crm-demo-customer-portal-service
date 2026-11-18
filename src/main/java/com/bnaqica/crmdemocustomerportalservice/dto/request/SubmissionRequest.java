package com.bnaqica.crmdemocustomerportalservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A new submission.
 *
 * @param type for example {@code CLAIM}; upper case letters, digits and underscores
 * @param region for example {@code EU}
 * @param data the submission's content, a JSON object
 * @param documentIds the caller's CLEAN documents to attach
 */
public record SubmissionRequest(
        @NotNull @Pattern(regexp = CODE) String type,
        @NotNull @Pattern(regexp = CODE) String region,
        @NotNull Map<String, Object> data,
        @Size(max = 20) List<@NotNull UUID> documentIds) {

    static final String CODE = "[A-Z][A-Z0-9_]{0,49}";
}

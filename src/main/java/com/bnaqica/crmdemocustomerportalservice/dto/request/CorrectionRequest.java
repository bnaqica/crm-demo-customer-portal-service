package com.bnaqica.crmdemocustomerportalservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A corrected version of a submission's data and documents; its type and region stay. */
public record CorrectionRequest(
        @NotNull Map<String, Object> data, @Size(max = 20) List<@NotNull UUID> documentIds) {}

package com.bnaqica.crmdemocustomerportalservice.dto.response;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalFeature;
import com.bnaqica.crmdemocustomerportalservice.dto.Tier;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/** The caller's own profile, from their token. */
@Builder
public record ProfileResponse(UUID sub, String email, String name, Tier tier, List<PortalFeature> features) {}

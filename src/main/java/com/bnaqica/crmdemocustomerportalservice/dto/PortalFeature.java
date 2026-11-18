package com.bnaqica.crmdemocustomerportalservice.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The portal features {@code GET /me} lists, each with the lowest tier that may use it. Endpoints of a STARTER
 * feature carry {@code @StarterFeature}; keep the two in step when adding a feature.
 */
@Getter
@RequiredArgsConstructor
public enum PortalFeature {
    PROFILE(Tier.STARTER),
    /** Uploading documents and seeing their scan status. */
    DOCUMENTS(Tier.STARTER),
    /** Creating, correcting and following submissions. */
    SUBMISSIONS(Tier.STANDARD);

    private final Tier tier;

    public boolean availableTo(Tier userTier) {
        return tier == Tier.STARTER || userTier == Tier.STANDARD;
    }
}

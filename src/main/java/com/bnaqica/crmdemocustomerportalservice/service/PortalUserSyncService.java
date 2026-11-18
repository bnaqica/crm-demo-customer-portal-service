package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;

/** Keeps the stored portal user in step with their token. */
public interface PortalUserSyncService {

    /**
     * Records the user on their first call (audited as {@code PORTAL_USER_REGISTERED}); on later calls, stores a
     * changed email and publishes {@code PortalUserEmailChanged}.
     */
    void sync(PortalPrincipal principal);
}

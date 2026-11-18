package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.response.ProfileResponse;

/** The caller's own profile. */
public interface ProfileService {

    /** Taken from the token; the provisioning filter has already stored a changed email. */
    ProfileResponse profile(PortalPrincipal principal);
}

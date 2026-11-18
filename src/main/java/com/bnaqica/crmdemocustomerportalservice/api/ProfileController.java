package com.bnaqica.crmdemocustomerportalservice.api;

import com.bnaqica.crmdemocustomerportalservice.config.StarterFeature;
import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.response.ProfileResponse;
import com.bnaqica.crmdemocustomerportalservice.service.ProfileService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The caller's own portal user. The table's other writes happen on the user's calls and in Keycloak, not here. */
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService service;

    @StarterFeature
    @GetMapping("/me")
    ProfileResponse me(@Parameter(hidden = true) PortalPrincipal principal) {
        return service.profile(principal);
    }
}

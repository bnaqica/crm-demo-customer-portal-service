package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalFeature;
import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.response.ProfileResponse;
import java.util.Arrays;
import org.springframework.stereotype.Service;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Override
    public ProfileResponse profile(PortalPrincipal principal) {
        return ProfileResponse.builder()
                .sub(principal.sub())
                .email(principal.email())
                .name(principal.name())
                .tier(principal.tier())
                .features(Arrays.stream(PortalFeature.values())
                        .filter(feature -> feature.availableTo(principal.tier()))
                        .toList())
                .build();
    }
}

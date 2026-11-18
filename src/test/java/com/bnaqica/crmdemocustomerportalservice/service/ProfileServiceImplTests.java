package com.bnaqica.crmdemocustomerportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalFeature;
import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.Tier;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfileServiceImplTests {

    private final ProfileService service = new ProfileServiceImpl();

    @Test
    void starterSeesOnlyStarterFeatures() {
        var profile = service.profile(
                new PortalPrincipal(UUID.randomUUID(), "a@acme.example", "A", Tier.STARTER, Instant.now()));

        assertThat(profile.features()).allMatch(feature -> feature.getTier() == Tier.STARTER);
    }

    @Test
    void standardSeesEveryFeature() {
        var profile = service.profile(
                new PortalPrincipal(UUID.randomUUID(), "a@acme.example", "A", Tier.STANDARD, Instant.now()));

        assertThat(profile.features()).containsExactlyInAnyOrder(PortalFeature.values());
    }

    @Test
    void profileComesFromThePrincipal() {
        UUID sub = UUID.randomUUID();

        var profile = service.profile(new PortalPrincipal(sub, "a@acme.example", "A B", Tier.STARTER, Instant.now()));

        assertThat(profile.sub()).isEqualTo(sub);
        assertThat(profile.email()).isEqualTo("a@acme.example");
        assertThat(profile.name()).isEqualTo("A B");
        assertThat(profile.tier()).isEqualTo(Tier.STARTER);
    }
}

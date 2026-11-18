package com.bnaqica.crmdemocustomerportalservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.Tier;
import com.bnaqica.crmdemocustomerportalservice.publisher.AuditEventPublisher;
import com.bnaqica.crmdemocustomerportalservice.publisher.PortalIntakePublisher;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalUserSyncServiceImplTests {

    private final PortalUserService users = mock(PortalUserService.class);
    private final AuditEventPublisher audit = mock(AuditEventPublisher.class);
    private final PortalIntakePublisher intake = mock(PortalIntakePublisher.class);
    private final PortalUserSyncService service = new PortalUserSyncServiceImpl(users, audit, intake);

    private final PortalPrincipal principal =
            new PortalPrincipal(UUID.randomUUID(), "new@acme.example", "Test User", Tier.STARTER, Instant.now());

    @Test
    void firstCallRecordsTheUserAndAuditsTheRegistration() {
        when(users.createIfAbsent(principal.sub(), principal.email(), principal.tokenIssuedAt()))
                .thenReturn(true);

        service.sync(principal);

        verify(audit).portalUserRegistered(principal.sub(), principal.email());
        verify(intake, never()).emailChanged(any(), anyString());
    }

    @Test
    void laterCallWithTheSameEmailPublishesNothing() {
        when(users.createIfAbsent(principal.sub(), principal.email(), principal.tokenIssuedAt()))
                .thenReturn(false);
        when(users.changeEmail(principal.sub(), principal.email(), principal.tokenIssuedAt()))
                .thenReturn(false);

        service.sync(principal);

        verify(audit, never()).portalUserRegistered(any(), anyString());
        verify(intake, never()).emailChanged(any(), anyString());
    }

    @Test
    void changedEmailIsPublished() {
        when(users.createIfAbsent(principal.sub(), principal.email(), principal.tokenIssuedAt()))
                .thenReturn(false);
        when(users.changeEmail(principal.sub(), principal.email(), principal.tokenIssuedAt()))
                .thenReturn(true);

        service.sync(principal);

        verify(intake).emailChanged(principal.sub(), principal.email());
    }
}

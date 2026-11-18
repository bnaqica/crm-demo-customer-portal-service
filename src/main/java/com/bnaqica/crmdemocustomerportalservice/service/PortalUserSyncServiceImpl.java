package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.publisher.AuditEventPublisher;
import com.bnaqica.crmdemocustomerportalservice.publisher.PortalIntakePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalUserSyncServiceImpl implements PortalUserSyncService {

    private final PortalUserService users;
    private final AuditEventPublisher audit;
    private final PortalIntakePublisher intake;

    @Override
    @Transactional
    public void sync(PortalPrincipal principal) {
        if (users.createIfAbsent(principal.sub(), principal.email(), principal.tokenIssuedAt())) {
            audit.portalUserRegistered(principal.sub(), principal.email());
            log.info("Registered portal user {}", principal.sub());
        } else if (users.changeEmail(principal.sub(), principal.email(), principal.tokenIssuedAt())) {
            intake.emailChanged(principal.sub(), principal.email());
            log.info("Email of portal user {} changed", principal.sub());
        }
    }
}

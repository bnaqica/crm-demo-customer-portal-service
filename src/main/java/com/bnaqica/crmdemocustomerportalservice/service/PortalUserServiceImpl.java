package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.entity.PortalUser;
import com.bnaqica.crmdemocustomerportalservice.repository.PortalUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortalUserServiceImpl implements PortalUserService {

    private final PortalUserRepository repository;
    private final Clock clock;

    @Override
    @Transactional
    public boolean createIfAbsent(UUID sub, String email, Instant tokenIssuedAt) {
        return repository.insertIfAbsent(sub, email, tokenIssuedAt, Instant.now(clock)) == 1;
    }

    @Override
    @Transactional
    public boolean changeEmail(UUID sub, String email, Instant tokenIssuedAt) {
        return repository.updateEmailIfChanged(sub, email, tokenIssuedAt, Instant.now(clock)) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PortalUser> find(UUID sub) {
        return repository.findById(sub);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(UUID sub) {
        repository.lockBySub(sub).orElseThrow();
    }
}

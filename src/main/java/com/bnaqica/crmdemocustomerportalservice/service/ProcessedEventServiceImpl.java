package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.repository.ProcessedEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ProcessedEventServiceImpl implements ProcessedEventService {

    private final ProcessedEventRepository repository;
    private final Clock clock;

    @Override
    public boolean markProcessed(UUID eventId) {
        return repository.insertIfAbsent(eventId, Instant.now(clock)) == 1;
    }
}

package com.bnaqica.crmdemocustomerportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bnaqica.crmdemocustomerportalservice.entity.OutboxEvent;
import com.bnaqica.crmdemocustomerportalservice.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxServiceImplTests {

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final Instant now = Instant.parse("2026-11-12T10:00:00Z");
    private final OutboxServiceImpl service = new OutboxServiceImpl(repository, Clock.fixed(now, ZoneOffset.UTC));

    private OutboxEvent eventWithAttempts(int attempts) {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .topic("portal-intake")
                .messageKey("k")
                .payload("{}")
                .createdAt(now)
                .attempts(attempts)
                .build();
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));
        return event;
    }

    @Test
    void failureBelowTheLimitIsCountedNotParked() {
        OutboxEvent event = eventWithAttempts(OutboxServiceImpl.MAX_ATTEMPTS - 2);

        service.recordFailure(event.getId(), "timeout");

        assertThat(event.getAttempts()).isEqualTo(OutboxServiceImpl.MAX_ATTEMPTS - 1);
        assertThat(event.getLastError()).isEqualTo("timeout");
        assertThat(event.getParkedAt()).isNull();
    }

    @Test
    void lastAllowedFailureParksTheEvent() {
        OutboxEvent event = eventWithAttempts(OutboxServiceImpl.MAX_ATTEMPTS - 1);

        service.recordFailure(event.getId(), "timeout");

        assertThat(event.getParkedAt()).isEqualTo(now);
    }
}

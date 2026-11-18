package com.bnaqica.crmdemocustomerportalservice.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bnaqica.crmdemocustomerportalservice.entity.OutboxEvent;
import com.bnaqica.crmdemocustomerportalservice.service.OutboxService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.support.TransactionOperations;

/** Relay behaviour with Kafka and the outbox table mocked. */
class OutboxRelayTests {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    private final OutboxService outbox = mock(OutboxService.class);
    private final OutboxRelay relay =
            new OutboxRelay(outbox, kafka, TransactionOperations.withoutTransaction(), Clock.systemUTC());

    private static OutboxEvent event(String key, String type) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .topic("portal-intake")
                .messageKey(key)
                .eventType(type)
                .payload("{}")
                .createdAt(Instant.now())
                .build();
    }

    @SuppressWarnings("unchecked")
    private void sendSucceeds() {
        when(kafka.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsTheEventTypeAsTheTypeHeader() {
        OutboxEvent event = event("a", "PortalUserEmailChanged");
        sendSucceeds();
        when(outbox.lockNextUnpublished()).thenReturn(Optional.of(event), Optional.empty());

        relay.relay();

        ArgumentCaptor<ProducerRecord<String, String>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafka).send(sent.capture());
        assertThat(sent.getValue().key()).isEqualTo("a");
        assertThat(new String(sent.getValue().headers().lastHeader("type").value(), StandardCharsets.UTF_8))
                .isEqualTo("PortalUserEmailChanged");
        verify(outbox).markPublished(eq(event), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void eventWithoutTypeHasNoTypeHeader() {
        OutboxEvent event = event("a", null);
        sendSucceeds();
        when(outbox.lockNextUnpublished()).thenReturn(Optional.of(event), Optional.empty());

        relay.relay();

        ArgumentCaptor<ProducerRecord<String, String>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafka).send(sent.capture());
        assertThat(sent.getValue().headers().lastHeader("type")).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void failedEventIsRecordedAndStopsTheRun() {
        OutboxEvent failing = event("a", null);
        OutboxEvent next = event("b", null);
        when(kafka.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("record too large")));
        when(outbox.lockNextUnpublished()).thenReturn(Optional.of(failing), Optional.of(next));

        relay.relay();

        verify(outbox).recordFailure(eq(failing.getId()), anyString());
        verify(outbox, never()).markPublished(any(), any());
        verify(kafka).send(any(ProducerRecord.class)); // not tried again for `next` in this run
    }
}

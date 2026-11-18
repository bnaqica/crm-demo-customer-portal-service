package com.bnaqica.crmdemocustomerportalservice.publisher;

import com.bnaqica.crmdemocustomerportalservice.entity.OutboxEvent;
import com.bnaqica.crmdemocustomerportalservice.exception.OutboxPublishException;
import com.bnaqica.crmdemocustomerportalservice.service.OutboxService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Publishes committed outbox events to Kafka, oldest first, one event per short transaction, so a sent event is
 * marked at once. An event Kafka refuses is counted and ends the run; after 5 failures it is parked and the events
 * behind it carry on. An event's type, when it has one, goes in the {@code type} header.
 *
 * <p>Delivery is at least once (a crash between sending and marking sends again), and with several instances order
 * across events isn't guaranteed. Consumers drop duplicates by event id.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    static final String TYPE_HEADER = "type";
    private static final int MAX_EVENTS_PER_RUN = 100;

    private final OutboxService outbox;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionOperations transactions;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${portal.outbox.relay-interval:1s}")
    public void relay() {
        for (int i = 0; i < MAX_EVENTS_PER_RUN; i++) {
            try {
                Boolean sent = transactions.execute(
                        status -> outbox.lockNextUnpublished().map(this::send).orElse(false));
                if (!Boolean.TRUE.equals(sent)) {
                    return; // nothing left
                }
            } catch (OutboxPublishException ex) {
                log.error(ex.getMessage(), ex.getCause());
                transactions.executeWithoutResult(
                        status -> outbox.recordFailure(ex.getEventId(), String.valueOf(ex.getCause())));
                return;
            }
        }
    }

    private boolean send(OutboxEvent event) {
        var record = new ProducerRecord<>(event.getTopic(), event.getMessageKey(), event.getPayload());
        if (event.getEventType() != null) {
            record.headers().add(TYPE_HEADER, event.getEventType().getBytes(StandardCharsets.UTF_8));
        }
        try {
            kafka.send(record).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OutboxPublishException(event.getId(), event.getTopic(), ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new OutboxPublishException(event.getId(), event.getTopic(), ex);
        }
        outbox.markPublished(event, Instant.now(clock));
        log.debug("Published outbox event {} to {}", event.getId(), event.getTopic());
        return true;
    }
}

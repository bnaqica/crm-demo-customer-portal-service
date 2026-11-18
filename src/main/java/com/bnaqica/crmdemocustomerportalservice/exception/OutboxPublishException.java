package com.bnaqica.crmdemocustomerportalservice.exception;

import java.util.UUID;
import lombok.Getter;

/** Kafka refused an outbox event. The relay counts the failure and parks the event after too many. */
@Getter
public class OutboxPublishException extends RuntimeException {

    private final UUID eventId;
    private final String topic;

    public OutboxPublishException(UUID eventId, String topic, Throwable cause) {
        super("Couldn't publish outbox event " + eventId + " to " + topic, cause);
        this.eventId = eventId;
        this.topic = topic;
    }
}

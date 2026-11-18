package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.entity.OutboxEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** CRUD for {@code outbox_event}. Every method joins the caller's transaction and fails without one. */
public interface OutboxService {

    /** @param eventType sent as the {@code type} header, or null for none */
    void add(String topic, String key, String eventType, String payload);

    /** The oldest event to publish, locked for this transaction; empty when there is none. */
    Optional<OutboxEvent> lockNextUnpublished();

    void markPublished(OutboxEvent event, Instant at);

    /** Counts a failed attempt and parks the event once it has failed too often. */
    void recordFailure(UUID id, String error);
}

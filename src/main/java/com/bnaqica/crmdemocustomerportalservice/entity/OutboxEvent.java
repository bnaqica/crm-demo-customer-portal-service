package com.bnaqica.crmdemocustomerportalservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An event waiting to be published to Kafka, already published ({@code publishedAt} set), or parked after repeated failures. */
@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
@AllArgsConstructor
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    /** Sent as the {@code type} header; null for topics that carry one kind of event. */
    @Column(name = "event_type")
    private String eventType;

    @Column(nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    /** Failed publish attempts so far. */
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    /** Set once the relay gives up on the event; it then needs a person to look at it. */
    @Column(name = "parked_at")
    private Instant parkedAt;
}

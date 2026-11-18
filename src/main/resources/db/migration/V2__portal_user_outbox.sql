-- A portal user, created on their first authenticated call. Keyed by the portal realm's token `sub`.
CREATE TABLE portal_user (
    sub        UUID PRIMARY KEY,
    email      VARCHAR(320) NOT NULL,
    -- `iat` of the token the stored email came from, so an older token still in use can't set it back
    token_issued_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

-- Transactional outbox: events are written in the same transaction as the change they describe, and a relay
-- publishes them to Kafka afterwards, so an event exists only if its change committed. An event Kafka keeps
-- refusing is retried a few times, then parked so it stops blocking the events behind it.
CREATE TABLE outbox_event (
    id           UUID PRIMARY KEY,
    topic        VARCHAR(255) NOT NULL,
    message_key  VARCHAR(255) NOT NULL,
    event_type   VARCHAR(100),             -- sent as the `type` header when set
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ,
    attempts     INTEGER      NOT NULL DEFAULT 0,
    last_error   TEXT,
    parked_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_event_unpublished ON outbox_event (created_at) WHERE published_at IS NULL AND parked_at IS NULL;

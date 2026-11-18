-- An uploaded document. The file itself is in private object storage under `storage_key`; only CLEAN documents can
-- be used in a submission.
CREATE TABLE document (
    id           UUID PRIMARY KEY,
    owner_sub    UUID         NOT NULL REFERENCES portal_user (sub),
    storage_key  VARCHAR(255) NOT NULL UNIQUE,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes   BIGINT       NOT NULL CHECK (size_bytes > 0),   -- as declared when the upload was requested
    status       VARCHAR(20)  NOT NULL,                          -- PENDING, CLEAN, INFECTED, REJECTED
    sha256       VARCHAR(64),                                    -- set by the scan
    created_at   TIMESTAMPTZ  NOT NULL,
    scanned_at   TIMESTAMPTZ
);

CREATE INDEX idx_document_owner_status ON document (owner_sub, status);

-- A submission and its current status. What was submitted is in submission_version, which is never changed.
CREATE TABLE submission (
    id               UUID PRIMARY KEY,
    owner_sub        UUID        NOT NULL REFERENCES portal_user (sub),
    type             VARCHAR(50) NOT NULL,
    region           VARCHAR(50) NOT NULL,
    status           VARCHAR(20) NOT NULL,   -- SUBMITTED, ACCEPTED, REJECTED
    rejection_reason TEXT,
    current_version  INTEGER     NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    lock_version     BIGINT      NOT NULL DEFAULT 0   -- optimistic locking
);

CREATE INDEX idx_submission_owner_created ON submission (owner_sub, created_at DESC);

-- One immutable version of a submission's data: version 1 when created, a new one for each correction.
CREATE TABLE submission_version (
    id            UUID PRIMARY KEY,
    submission_id UUID        NOT NULL REFERENCES submission (id),
    version       INTEGER     NOT NULL,
    data          TEXT        NOT NULL,   -- a JSON object
    created_at    TIMESTAMPTZ NOT NULL,
    UNIQUE (submission_id, version)
);

CREATE TABLE submission_version_document (
    submission_version_id UUID NOT NULL REFERENCES submission_version (id),
    document_id           UUID NOT NULL REFERENCES document (id),
    PRIMARY KEY (submission_version_id, document_id)
);

-- Status events already applied, so a redelivered event changes nothing.
CREATE TABLE processed_event (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);

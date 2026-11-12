# Design

## Context

Depends on `add-portal-accounts` (access rules, outbox). Event contract matches core-service `add-submission-intake` design. See proposal.md.

## Decisions

- **Storage**: S3 API through AWS SDK v2 in `client`; LocalStack locally. Keys `uploads/{sub}/{documentId}`; bucket private, no public ACLs.
- **Scan flow**: S3 object-created notification → this service → ClamAV (`clamd` over TCP) → status CLEAN or INFECTED. Alternative: scanning in the request — would make uploads pass through the API.
- **Quota** computed from the sum of CLEAN + PENDING document sizes per user; limits in `@ConfigurationProperties` per tier.
- **Versions**: `submission` holds owner and current status; `submission_version` holds immutable data and document links.
- **Outbox** reused from `add-portal-accounts`; status consumer with dedup table and DLQ handler.

## Risks / Trade-offs

- [Pre-signed PUT can't enforce size by itself] → sign with `Content-Length` and re-check the object size after upload; delete oversize objects.

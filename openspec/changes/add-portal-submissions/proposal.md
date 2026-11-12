# Proposal

## Why

External users need to submit data and documents to the business and see what happened to them, without ever touching CRM APIs. The portal stores submissions itself and exchanges them with the CRM through events.

## What Changes

- Owner-only submission API: create, list, view, and correct by new version.
- Document upload through pre-signed URLs to private object storage, with size/type limits, virus scanning and per-user quotas.
- `SubmissionReceived` published to `portal-intake` with the submission in one transaction (outbox).
- Status events from `submission-status` update what the user sees.

## Capabilities

### New Capabilities
- `portal-submissions`: the user's own submissions and their status.
- `document-uploads`: secure upload of documents.
- `submission-events`: the event exchange with the CRM.

## Impact

- Tables `submission`, `submission_version`, `document`, `processed_event`.
- Object storage (S3 API; LocalStack from `local-docker` locally) and a virus scanner (ClamAV container).
- Kafka: publishes `portal-intake`, `audit-events`; consumes `submission-status`.
- Event contract shared with core-service `add-submission-intake`.

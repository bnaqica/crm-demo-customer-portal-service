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

## Implementation decisions

Recorded during `/opsx:apply`.

- **Upload notifications through SQS.** S3 can't call this service directly, so the bucket sends `ObjectCreated`
  notifications to an SQS queue (`portal-uploads`) that `listener/UploadNotificationListener` polls. A notification
  is deleted from the queue only after its document is handled, so a failed scan is retried after the queue's
  visibility timeout. LocalStack provides both S3 and SQS locally.
- **Object size re-checked** before scanning: an object larger than declared, or above the per-file limit, is
  deleted and its document marked `REJECTED`. Only `CLEAN` documents can be used in a submission.
- **SHA-256** is computed while the object streams to ClamAV, because `SubmissionReceived` carries it.
- **Clients that aren't HTTP.** `clamd` speaks its own TCP protocol and S3/SQS go through the AWS SDK, so these
  clients can't be declarative `@HttpExchange` interfaces. Each sits in `client` behind a small interface
  (`VirusScanner`, `DocumentStorage`, `UploadQueue`).
- **Document links** use a join table `submission_version_document` (version → document), alongside the four tables
  in the proposal.
- **Data** is a JSON object stored with each version. The fixed `SubmissionReceived` v1 contract doesn't carry it,
  so the CRM gets references only, as the event-contents requirement says.
- **Features.** `DOCUMENTS` (upload and view one's documents) is a starter feature, since starters have a document
  quota; `SUBMISSIONS` is standard-only. A `GET /documents/{id}` endpoint lets the uploader see the scan status
  before submitting, as the CRUD-controller rule requires for the `document` table.
- **Status by version.** A status event applies only when it names the submission's current version; one for an
  older version (overtaken by a correction) is recorded as processed and ignored. A correction resets the status to
  `SUBMITTED` and clears the rejection reason.
- **Status events checked:** schema version 1, status `ACCEPTED` or `REJECTED`, a known submission whose owner is
  the event's `portalUserId`. Anything else goes to `submission-status-dlq` without retries, and is logged.
- **Tables without a CRUD controller**: `submission_version` and `submission_version_document` are read and
  written only through their submission (`/submissions/{id}`, `/submissions/{id}/versions`), because versions are
  immutable; `processed_event` is infrastructure, like `outbox_event`. As in core-service, this is an exception to
  the "every table has a CRUD controller" rule that the developer should confirm.
- **Corrections of decided submissions** are allowed (the spec doesn't forbid them): an ACCEPTED or REJECTED
  submission goes back to SUBMITTED with the new version. To confirm with the developer and core-service.

## Code review

Reviewed with the `code-reviewer` agent. Fixed, each with a failing test first:
- Critical: the upload URL could replace a file already scanned CLEAN within its 10 minutes. The PUT is now signed
  with `If-None-Match: *`, so only the first upload succeeds (412 otherwise).
- Important: never-uploaded PENDING documents used quota forever. They now count only while the URL works plus
  `scan-grace` (1 hour).
- Important: upload scanning and the outbox relay shared one scheduler thread, so a slow scan held up events.
  `spring.task.scheduling.pool.size` is 2.
- Important: concurrent corrections failed with 500. `correct` now locks the submission row; both succeed as
  consecutive versions.
- Minor, done: tests for the upload listener and the clamd protocol; storage and queue requirements documented.

Deferred (Minor): checking file signatures (magic bytes) against the declared type; an integration test for a PUT
of the wrong size; `@WebMvcTest` slices for the two new controllers (their behaviour is covered by MockMvc
integration tests); dead-lettering notifications in code rather than by the queue's redrive policy.

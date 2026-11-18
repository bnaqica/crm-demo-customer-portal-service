# Submissions, documents and the CRM event contract

Portal users submit data and documents to the business through this API; the portal exchanges them with the CRM
(core-service) as Kafka events. Access rules (tokens, starter and standard features) are in
[portal-access.md](portal-access.md).

## Documents (starter feature)

Files never pass through the API. A user asks for an upload URL, sends the file straight to private object storage,
and the portal scans it.

| Call | Does |
|---|---|
| `POST /documents` `{fileName, contentType, size}` | Checks the rules below, records a `PENDING` document and returns `{documentId, status, uploadUrl, uploadHeaders, expiresAt}` |
| `PUT <uploadUrl>` with `uploadHeaders` | Uploads exactly `size` bytes, straight to storage, before `expiresAt` (at most 10 minutes). Only the first upload succeeds (`If-None-Match: *`); another gets 412, so a scanned file can't be swapped |
| `GET /documents/{id}`, `GET /documents` | The caller's documents and their status; another user's id is 404 |

Upload rules (422 when broken, defaults in brackets):

- Content type in `portal.uploads.allowed-content-types` (`application/pdf`, `image/png`, `image/jpeg`).
- Size at most `PORTAL_UPLOADS_MAX_FILE_SIZE` (10 MB).
- Quota: the caller's `CLEAN` documents, their `PENDING` ones still within the upload URL's lifetime plus
  `portal.uploads.scan-grace` (1 hour), and the new one must fit `PORTAL_UPLOADS_QUOTA_STARTER` (20 MB) or
  `PORTAL_UPLOADS_QUOTA_STANDARD` (200 MB). An abandoned upload stops counting after that.

After the upload, the bucket sends an `ObjectCreated` notification to the SQS queue
(`PORTAL_STORAGE_UPLOAD_QUEUE_URL`), and the portal:

1. checks the stored object's size against the declared size and the limit; larger objects are deleted and the
   document becomes `REJECTED`;
2. streams the object to ClamAV (`clamd`), computing its SHA-256 on the way;
3. marks it `CLEAN`, or deletes it and marks it `INFECTED`.

A notification that fails (ClamAV down, for example) stays on the queue and is retried after its visibility timeout.
Objects are stored under `uploads/{sub}/{documentId}` in a private bucket; nothing is ever publicly readable.

### Storage the environment must provide

- A bucket with all public access blocked.
- An SQS queue, and a bucket notification sending `s3:ObjectCreated:*` to it.
- On the queue: a visibility timeout longer than a batch of 10 scans (5 minutes is safe), and a redrive policy
  (for example `maxReceiveCount` 10) to a dead-letter queue, so a notification that can never be handled stops
  coming back. Its document then stays `PENDING` and stops counting towards the quota.
- ClamAV's `StreamMaxLength` at least `PORTAL_UPLOADS_MAX_FILE_SIZE`.

## Submissions (standard feature)

| Call | Does |
|---|---|
| `POST /submissions` `{type, region, data, documentIds}` | Creates a `SUBMITTED` submission, version 1, and sends `SubmissionReceived` |
| `POST /submissions/{id}/versions` `{data, documentIds}` | A correction: version n+1 with the same type and region, status back to `SUBMITTED`, and a new `SubmissionReceived` |
| `GET /submissions` | The caller's submissions, newest first, without versions |
| `GET /submissions/{id}` | One submission with its status, rejection reason and every version |

- `type` and `region` are codes such as `CLAIM` and `EU` (`[A-Z][A-Z0-9_]{0,49}`); `data` is a JSON object; at most
  20 documents, each the caller's own and `CLEAN` (otherwise 422).
- Submitted versions are never edited. Another user's submission is 404, for reads and corrections alike.
- The owner sees only the status (`SUBMITTED`, `ACCEPTED`, `REJECTED`) and the rejection reason from the CRM.

## Events

All events are JSON text with no Java type headers. Ids are UUID strings, `occurredAt` is ISO-8601 UTC and
`schemaVersion` is the number `1`. Contract shared with core-service (`add-submission-intake`).

### Published to `portal-intake`

Keyed by the portal user's `sub`, so one user's events stay in order; the `type` header names the event. Written to
the transactional outbox in the same transaction as the change and published after commit, at least once:
consumers drop duplicates by `eventId`.

| `type` header | Payload |
|---|---|
| `SubmissionReceived` | `{eventId, schemaVersion, occurredAt, portalUserId, email, submissionId, version, type, region, documents: [{documentId, storageKey, contentType, size, sha256}]}` |
| `PortalUserEmailChanged` | `{eventId, schemaVersion, occurredAt, portalUserId, email}` |

`SubmissionReceived` carries document references only, never file contents or the submission's `data`.

### Consumed from `submission-status`

`SubmissionAccepted` / `SubmissionRejected` v1:
`{eventId, schemaVersion, occurredAt, submissionId, version, portalUserId, status, reason?}`.

- `status` (`ACCEPTED` or `REJECTED`) decides what happens; the `type` header isn't needed.
- Applied once per `eventId` (`processed_event`, in the same transaction as the status change).
- Applied only to the submission's current version; a decision about an older version, overtaken by a
  correction, is recorded and ignored.
- An event with another schema version, an unknown status, an unknown submission, a `portalUserId` that isn't the
  owner, or a version that doesn't exist yet goes to `submission-status-dlq` straight away, and is logged. Other
  failures are retried 3 times a second apart first. Unreadable JSON goes to the DLQ straight away too.

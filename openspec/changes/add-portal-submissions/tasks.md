# Tasks

## 1. Data

- [ ] 1.1 Add `V3__submissions.sql` (submission, submission_version, document, processed_event) and verify the migration test

## 2. Documents

- [ ] 2.1 Add the S3 client and pre-signed upload endpoint with type, size and quota checks; verify the `document-uploads` request scenarios with LocalStack Testcontainers
- [ ] 2.2 Implement the scan flow with ClamAV; verify the infected-upload scenario with a ClamAV Testcontainer and the EICAR test file

## 3. Submissions

- [ ] 3.1 Implement create, list and view with owner-only filtering; verify the `portal-submissions` create and owner scenarios
- [ ] 3.2 Implement corrections as new versions; verify the correction scenario
- [ ] 3.3 Publish `SubmissionReceived` via the outbox; verify contents and the rollback scenario

## 4. Status

- [ ] 4.1 Consume `submission-status` with dedup and DLQ; verify the accepted and unknown-submission scenarios
- [ ] 4.2 Document the submissions API and event contract in `docs/`

## Workflow follow-up

- Archive the change with `/opsx:archive` after review.

# Spec Delta

## Purpose

Lets portal users upload documents safely, without files passing through the API or reaching anyone before they are scanned.

## ADDED Requirements

### Requirement: Pre-signed upload
The service SHALL issue a short-lived pre-signed upload URL for an allowed content type and declared size, to a private storage key owned by the user.

#### Scenario: Allowed file
- **WHEN** a user requests an upload for a 2 MB `application/pdf`
- **THEN** a pre-signed URL valid for at most 10 minutes and a document id are returned

#### Scenario: Disallowed type
- **WHEN** a user requests an upload for `application/x-msdownload`
- **THEN** the response is 422

### Requirement: Size and quota limits
The service SHALL reject uploads above the per-file limit, and uploads that would exceed the user's quota; starters SHALL have a lower quota than standard users.

#### Scenario: Quota exceeded
- **WHEN** a starter's new upload would exceed the starter quota
- **THEN** the response is 422

### Requirement: Virus scan
Every uploaded object SHALL be scanned; infected objects SHALL be deleted and marked INFECTED, and only CLEAN documents SHALL be usable in a submission.

#### Scenario: Infected upload
- **WHEN** an uploaded file is detected as infected
- **THEN** the object is deleted and the document status is INFECTED

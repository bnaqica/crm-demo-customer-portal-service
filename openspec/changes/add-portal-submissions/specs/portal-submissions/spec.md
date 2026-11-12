# Spec Delta

## Purpose

Lets external users create and follow their own submissions, and nobody else's.

## ADDED Requirements

### Requirement: Create submission
A standard user SHALL create a submission with type, region, data and references to their clean uploaded documents; it SHALL start with status SUBMITTED.

#### Scenario: Submission created
- **WHEN** a standard user submits type CLAIM, region EU and one clean document
- **THEN** a SUBMITTED submission owned by the user's `sub` is created

#### Scenario: Unscanned document
- **WHEN** a submission references a document that is not yet scanned clean
- **THEN** the response is 422

### Requirement: Owner only
Submissions SHALL be filtered by owner equal to the token's `sub`; requesting another user's submission SHALL return 404.

#### Scenario: Another user's submission
- **WHEN** user A requests user B's submission id
- **THEN** the response is 404

### Requirement: Immutable versions
Submitted data and documents SHALL NOT be edited; a correction SHALL create a new version of the same submission and be sent to the CRM again.

#### Scenario: Correction
- **WHEN** the owner corrects version 1 of a submission
- **THEN** version 2 exists, version 1 is unchanged, and a new `SubmissionReceived` event with version 2 is published

### Requirement: Status shown to owner
The owner SHALL see each submission's status (SUBMITTED, ACCEPTED, REJECTED) and rejection reason, and no other CRM data.

#### Scenario: Rejected submission
- **WHEN** the owner views a rejected submission
- **THEN** status REJECTED and the reason are shown

# Spec Delta

## Purpose

Exchanges submissions and their status with the CRM reliably, carrying only references to documents.

## ADDED Requirements

### Requirement: Submission and event saved together
Creating or correcting a submission SHALL store the `SubmissionReceived` event in the same transaction, and the event SHALL be published to `portal-intake` only after commit.

#### Scenario: Save fails
- **WHEN** saving the submission fails
- **THEN** no `SubmissionReceived` event is published

### Requirement: Event contents
`SubmissionReceived` SHALL carry event id, schema version, portal user id, email, submission id, version, type, region and document references, and SHALL NOT carry file contents.

#### Scenario: Event published
- **WHEN** a submission with two documents is created
- **THEN** the event lists two document references and contains no file bytes

### Requirement: Status updates applied
The service SHALL consume `SubmissionAccepted` and `SubmissionRejected` from `submission-status`, deduplicate by event id, and update the matching submission's status.

#### Scenario: Accepted event
- **WHEN** a `SubmissionAccepted` event for submission S arrives
- **THEN** S shows status ACCEPTED

#### Scenario: Unknown submission
- **WHEN** a status event names an unknown submission
- **THEN** the event goes to `submission-status-dlq` and is logged

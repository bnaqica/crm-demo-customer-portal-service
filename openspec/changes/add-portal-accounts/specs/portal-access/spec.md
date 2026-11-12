# Spec Delta

## Purpose

Makes sure only portal users reach the portal APIs, and only with the features their starter or standard role allows.

## ADDED Requirements

### Requirement: Trust only the portal realm
The service SHALL accept only JWTs from the `portal` realm and SHALL reject `crm-internal` and `crm-admin` tokens with 401.

#### Scenario: Internal token
- **WHEN** a request carries a `crm-internal` token
- **THEN** the response is 401

### Requirement: Starter and standard features
Endpoints SHALL declare whether they are starter features; starter features SHALL be allowed for `external-starter` and `external-standard`, all other endpoints only for `external-standard`.

#### Scenario: Starter uses standard feature
- **WHEN** a user with only `external-starter` calls a standard-only endpoint
- **THEN** the response is 403

### Requirement: Verified email required
Requests from users whose email is not verified SHALL be rejected with 403.

#### Scenario: Unverified user
- **WHEN** a token with `email_verified` false calls any portal endpoint
- **THEN** the response is 403

### Requirement: Portal user recorded on first call
On a user's first authenticated call the service SHALL create a portal user record keyed by the token `sub` and publish a registration audit event.

#### Scenario: First call
- **WHEN** a newly registered user makes their first request
- **THEN** a portal user record exists for their `sub` and an audit event with action `PORTAL_USER_REGISTERED` is published

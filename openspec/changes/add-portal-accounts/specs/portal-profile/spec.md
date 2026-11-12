# Spec Delta

## Purpose

Shows portal users their own profile and keeps the CRM's copy of their email current.

## ADDED Requirements

### Requirement: Own profile
`GET /me` SHALL return the caller's `sub`, email, name, tier (STARTER or STANDARD) and the features available to that tier, taken from the token, never from request parameters.

#### Scenario: Starter profile
- **WHEN** a starter user calls `/me`
- **THEN** tier is STARTER and only starter features are listed

### Requirement: Email change published
When a user's verified email in the token differs from the stored one, the service SHALL update the stored email and publish `PortalUserEmailChanged` to `portal-intake` through the outbox.

#### Scenario: Email changed in Keycloak
- **GIVEN** a stored email `old@acme.example`
- **WHEN** the user calls with a token whose verified email is `new@acme.example`
- **THEN** the stored email is `new@acme.example` and one `PortalUserEmailChanged` event is published

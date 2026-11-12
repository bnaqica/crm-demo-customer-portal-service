# Proposal

## Why

External users sign in to the `portal` realm only and must never reach CRM data. The portal backend needs its own access model: owner-only data, starter versus standard features, and a profile whose email changes reach the CRM.

## What Changes

- Resource server that trusts only `portal` realm tokens.
- Feature access by portal role: `external-starter` gets starter features, `external-standard` gets all portal features.
- A local portal user record created on first authenticated call and recorded as a registration audit event.
- `GET /me` and email change (verified through Keycloak) publishing `PortalUserEmailChanged` via the outbox.

## Capabilities

### New Capabilities
- `portal-access`: token trust, account status and starter/standard feature rules.
- `portal-profile`: the user's own profile and email changes.

## Impact

- Spring Kafka, tables `portal_user`, `outbox_event`; topics `portal-intake`, `audit-events`.
- A `portal` realm service-account client for profile updates is not needed: email changes go through Keycloak's own account flow.

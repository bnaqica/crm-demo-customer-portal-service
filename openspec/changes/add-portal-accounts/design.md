# Design

## Context

Fresh scaffold. The portal realm handles registration, CAPTCHA, email verification and MFA (realm config in `local-docker/keycloak`). See proposal.md.

## Decisions

- **Issuer `portal`**; realm roles `external-starter` / `external-standard` mapped to authorities. `@StarterFeature` marks endpoints open to starters; everything else requires `ROLE_EXTERNAL_STANDARD` by default (deny by default).
- **Status** is not looked up live: UMS disables accounts in Keycloak and revokes sessions, and access tokens are short (5 min). Accepted trade-off for a portal with no CRM data.
- **First-call provisioning** in a `OncePerRequestFilter` after authentication, upserting on `sub`.
- **Email sync** compares on each `/me` call and on any write; publishing via the outbox shared with submissions.
- Email changes themselves happen in Keycloak's account console (verification built in); the portal doesn't accept email in requests.

## Risks / Trade-offs

- [Email change unseen until the user's next call] → acceptable; CRM shows the last known email.

## Implementation decisions

Recorded during `/opsx:apply`.

- **Sync on every allowed call.** The provisioning filter runs after authorization on every request and both creates
  the user and compares the email, which covers "each `/me` call and any write". One indexed read or single-statement
  upsert per call. Denied requests (403) never create a row.
- **Race-safe writes.** `INSERT ... ON CONFLICT DO NOTHING` and `UPDATE ... WHERE email <> :email` report whether
  this call made the change, so concurrent calls publish one registration audit event and one email event.
- **Features.** `/me` lists features from `dto/PortalFeature` (each with its lowest tier); the endpoints of a
  starter feature carry `@StarterFeature`. Only `PROFILE` exists in this change; submissions adds its own.
- **No CRUD controller for `outbox_event`**: like the audit table it is infrastructure, never changed through an
  API, so it's an exception to the "every table has a CRUD controller" rule, as in the UMS. `portal_user`'s API is
  `GET /me`: users see only themselves, rows are created by the first call, and email changes come from Keycloak.
- **Outbox `event_type`.** The outbox stores an optional event type, sent as the `type` Kafka header, because the
  core-service contract tells event kinds on `portal-intake` apart by that header. Events on `portal-intake` are
  keyed by the portal user's `sub`, so one user's events stay in order.
- **CORS** for the Portal UI (`http://localhost:5174`), set by `PORTAL_CORS_ALLOWED_ORIGINS`. Added at the request
  of the session coordinating the demo; not in the original proposal.
- **Starter tokens in tests.** The generated test realm has only a standard user; tests create starters and changed
  emails through Keycloak's admin API (`support/KeycloakTestAdmin`). Unverified-email tokens can't be obtained by a
  password grant from a realm with `verifyEmail`, so that rule is tested with mock tokens.
- **Stale tokens.** `portal_user.token_issued_at` stores the `iat` of the token the email came from; an email is
  only replaced by one from a newer token, so a 5-minute-old token on another device can't set it back.
  (Added to `V2` before it was ever applied or committed.)
- **Error dispatches** to `/error` are permitted, so a failure in the chain stays a 500 for starters rather than
  becoming 403. A token without an `email` claim is denied (403), like an unverified one.

## Code review

Reviewed with the `code-reviewer` agent. Fixed: starter errors turning into 403, stale tokens flipping the email,
missing `email` claim, README row for `PORTAL_OUTBOX_RELAY_INTERVAL`, tests for outbox parking. Deferred (Minor):
an alert when the relay parks an event (it logs at error level; the UMS raises an alert through its own
`AlertService`, which this service doesn't have) and using `HandlerMappingIntrospector` instead of the handler
mapping. The `spring.kafka.consumer` settings stay because `add-portal-submissions` adds the first listener. The
exceptions to the "every table has a CRUD controller" rule (`outbox_event`, `portal_user`) need the developer's
confirmation.

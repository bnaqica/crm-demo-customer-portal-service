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

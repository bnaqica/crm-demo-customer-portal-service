# Tasks

## 1. Access

- [x] 1.1 Add Spring Kafka; configure the `portal` issuer and role mapping; verify internal and admin tokens get 401
- [x] 1.2 Add `@StarterFeature` with standard-by-default and the verified-email rule; verify the `portal-access` scenarios

## 2. Profile

- [x] 2.1 Add `V2__portal_user_outbox.sql` and the first-call provisioning filter with the registration audit event; verify the first-call scenario
- [x] 2.2 Implement `GET /me` and email sync with `PortalUserEmailChanged` through the outbox; verify both `portal-profile` scenarios
- [x] 2.3 Document portal access rules in `docs/`

## Workflow follow-up

- Archive the change with `/opsx:archive` after review.

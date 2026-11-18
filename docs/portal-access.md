# Portal access rules

Who can call this service, and what each caller can do. The code is in `config/SecurityConfig`,
`config/PortalAccessAuthorizationManager` and `config/PortalUserSyncFilter`.

## Tokens

- Only access tokens from the Keycloak **`portal`** realm are accepted (`JWT_ISSUER_URI`). Tokens from
  `crm-internal` or `crm-admin` fail issuer validation and get **401**, as do missing, expired or forged tokens.
- `/actuator/health` is the only endpoint open without a token.
- Account status isn't looked up on each call. The UMS disables accounts in Keycloak and revokes their sessions, and
  portal access tokens last 5 minutes, so a disabled user loses access within that time.

## Rules for each request

Checked in this order, after the token is validated:

| Rule | Otherwise |
|---|---|
| The token's `email_verified` claim is `true` | 403 |
| The caller has `external-standard`, or has `external-starter` and the endpoint is a starter feature | 403 |

- An endpoint is a **starter feature** when its method or controller is annotated `@StarterFeature`. Every other
  endpoint, including a path with no endpoint, needs `external-standard` (deny by default).
- A user with both roles is treated as standard.

| Feature | Endpoints | Starter | Standard |
|---|---|---|---|
| `PROFILE` | `GET /me` | yes | yes |
| `DOCUMENTS` | `/documents/**` | yes | yes |
| `SUBMISSIONS` | `/submissions/**` | no | yes |

When adding an endpoint, decide whether it's a starter feature, and keep `dto/PortalFeature` (what `/me` lists) in
step with the `@StarterFeature` annotations.

## Ownership

The caller's identity comes only from the token: controllers receive a `PortalPrincipal` (the `sub`, email, name and
tier), never a user id from the request. Data a user creates is owned by their `sub`.

## Portal user records and email changes

Every request that passes the rules above syncs the caller's `portal_user` row, before the endpoint runs:

- **First call:** a row is created for the `sub`, and an audit event with action `PORTAL_USER_REGISTERED` goes to
  `audit-events`. Two simultaneous first calls create one row and one event.
- **Email changed:** users change their email in Keycloak's account console, which verifies the new address. When
  the token's email differs from the stored one, the row is updated and `PortalUserEmailChanged` v1 goes to
  `portal-intake` (`type` header `PortalUserEmailChanged`, keyed by the `sub`). The CRM sees the change on the user's
  next call, not at the moment it is made in Keycloak.

Denied requests don't create or change a row. Events go through the transactional outbox (`outbox_event`), so they
are published only if the change commits; consumers must drop duplicates by `eventId`.

## Browser access (CORS)

Browser origins in `PORTAL_CORS_ALLOWED_ORIGINS` (the Portal UI, `http://localhost:5174` locally) may call the API
with an `Authorization` header. Other origins' preflight requests get 403.

## Local profile

The `local` profile turns authentication off. Endpoints that need the caller, such as `/me`, then answer 401,
because there is no token to take the user from.

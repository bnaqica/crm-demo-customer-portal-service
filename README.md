# crm-demo-customer-portal-service

Backend for the Customer Portal: external users from the Keycloak `portal` realm manage their profile, upload documents (pre-signed S3 URLs, virus-scanned with ClamAV) and make submissions, which reach the CRM as Kafka events through a transactional outbox. See `docs/`.

| | |
|---|---|
| Port | 8082 |
| Stack | Java 25, Spring Boot 4.1, Gradle 9.9 (wrapper), PostgreSQL, Flyway, JUnit, Testcontainers, Spring Security (JWT), springdoc, Spotless, JaCoCo, Lombok, SLF4J (Logback), Jib |

## Prerequisites

- A JDK 17+ to run Gradle. The Java 25 toolchain is downloaded automatically by Gradle (foojay resolver)
  if you don't have it.
- In your IDE, the Lombok plugin with annotation processing enabled.
- For the feature workflow in Claude Code: Node.js 22.12+ and OpenSpec (`npm install -g @fission-ai/openspec@latest`).
- Docker running, for the local database and the Testcontainers tests.

## Run locally

```bash
./gradlew bootRun
```

This uses the `local` profile, which turns off authentication. Swagger UI is at
<http://localhost:8082/swagger-ui.html>.

## Environment variables

Needed everywhere except the `local` profile.

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC URL of the database |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_ISSUER_URI` | The `portal` realm issuer, for example `http://localhost:8180/realms/portal`; tokens from any other realm get 401 |
| `PORTAL_CORS_ALLOWED_ORIGINS` | Comma-separated browser origins allowed to call the API, for example `http://localhost:5174` (the Portal UI) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers, for example `localhost:9092` |
| `PORTAL_STORAGE_BUCKET` | Private bucket for uploaded documents |
| `PORTAL_STORAGE_REGION` | Its region, for example `eu-central-1` |
| `PORTAL_STORAGE_ENDPOINT` | Optional; the S3/SQS endpoint for LocalStack, for example `http://localhost:4566`. Empty for AWS |
| `PORTAL_STORAGE_ACCESS_KEY`, `PORTAL_STORAGE_SECRET_KEY` | Optional static credentials (LocalStack); empty to use the AWS default credentials chain |
| `PORTAL_STORAGE_UPLOAD_QUEUE_URL` | SQS queue that receives the bucket's object-created notifications |
| `PORTAL_CLAMAV_HOST`, `PORTAL_CLAMAV_PORT` | ClamAV `clamd` (port default `3310`) |
| `PORTAL_UPLOADS_MAX_FILE_SIZE` | Optional; largest file (default `10MB`) |
| `PORTAL_UPLOADS_URL_TTL` | Optional; how long an upload URL works, at most `10m` (default `10m`) |
| `PORTAL_UPLOADS_QUOTA_STARTER`, `PORTAL_UPLOADS_QUOTA_STANDARD` | Optional; storage per user (defaults `20MB`, `200MB`) |
| `portal.uploads.allowed-content-types`, `portal.uploads.scan-grace`, `portal.uploads.poll-interval` | Optional settings with defaults (`application/pdf,image/png,image/jpeg`; `1h`; `1s`), see `docs/submissions.md` |
| `PORTAL_OUTBOX_RELAY_INTERVAL` | Optional; how often the outbox relay publishes waiting events (default `1s`) |

## Test and format

```bash
./gradlew build
```

The build runs the tests, writes a coverage report to `build/reports/jacoco/test/html/index.html`, and fails if
the code isn't formatted. To format it:

```bash
./gradlew spotlessApply
```

## Build a container (Jib)

```bash
./gradlew jibDockerBuild
```

To push to a registry instead:

```bash
IMAGE_REGISTRY=ghcr.io/my-org ./gradlew jib
```

## Contributing

Package layout and conventions are in `CLAUDE.md` and `.claude/rules/`.

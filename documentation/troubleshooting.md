# Troubleshooting

## First checks

Run:

```bash
./gradlew healthCheck
./gradlew diagnostics
```

If a Gradle build fails, the root build prints a failure banner with:

- the failure message
- Gradle and Java versions
- OS information
- the command that was executed
- a reminder to include relevant module logs and report paths

## Upload problems

### Unsupported file type

Symptoms:

- upload returns `415`
- UI shows a supported-file-types error

Checks:

- use only `.pdf`, `.md`, or `.txt`
- confirm the filename extension matches the content type

### File too large

Symptoms:

- upload returns `413`

Checks:

- keep files under `41943040` bytes
- verify frontend and backend max-file-size configuration match

### Duplicate or invalid upload

Checks:

- confirm the user exists and is active
- confirm the file contains bytes and is not empty
- remember duplicate detection uses a SHA-256 content hash

## Processing problems

### Document stuck or failed

Checks:

- confirm extracted text contains at least 10 readable characters
- for PDFs, confirm the file is not password protected
- remember OCR is not implemented for scanned PDFs
- review admin progress for `failureReason`

Example failure scenarios:

- a scanned image PDF with no embedded text becomes `FAILED`
- a markdown file containing only formatting characters becomes `FAILED`

## Chat problems

### No answer found

Checks:

- confirm at least one document is `READY`
- confirm the user is allowed to query the matching documents
- for standard users, only their own `READY` documents are searched
- verify the question overlaps with extracted content

### Timeout

Checks:

- the default timeout is `20000` ms
- large document sets or slower local environments may trigger `408`

### Identity errors

The current identity contract is inconsistent by endpoint:

- upload uses multipart `userId`
- list and admin progress use `X-User-Id`
- chat expects an authenticated principal UUID

If one flow works and another fails, verify the correct identity mechanism for that endpoint.

## Infrastructure problems

### Compose service fails to start

Checks:

- run `docker compose ps`
- inspect logs with `docker compose logs <service>`
- confirm ports `3000`, `8080`, and `11434` are free

### Weaviate or Ollama confusion

The compose stack provisions both services, but current app behavior does not depend on live Weaviate retrieval or live LLM generation.

Today the backend uses:

- an in-memory vector store
- a heuristic local answer generator

This means infrastructure health can be green even when semantic search behavior does not reflect a real external vector database or model.

## Native dev environment problems

### Podman service stack is unhealthy

Checks:

- run `./status-dev-services.sh`
- run `./troubleshoot-dev-services.sh` for port, log, and connectivity diagnostics
- confirm `.env.dev` values match the expected local ports
- verify PostgreSQL `5432`, Weaviate `8080`, Keycloak `8180`, Redis `6379`, and Ollama `11434` are free

The service scripts now support `podman-compose`, `docker-compose`, and `docker compose`, so verify which runtime was selected before debugging container state.

### Startup fails before services become ready

Checks:

- `./start-dev-services.sh` now waits for PostgreSQL, Weaviate, and Keycloak readiness before reporting success
- if a readiness check fails, inspect the service logs printed by the script first
- verify the required bootstrap files exist, especially the Weaviate schema files and Keycloak realm import file

Useful commands:

```bash
./status-dev-services.sh
./troubleshoot-dev-services.sh
./stop-dev-services.sh --clean
./start-dev-services.sh
```

### Keycloak login expectations do not match the UI

Checks:

- Keycloak realm setup exists for service integration and backend OIDC configuration
- the frontend currently uses a dev auth stub in `frontend/src/config/keycloak.js`
- in debug mode the frontend auto-logs in with a fake `dev-token` and defaults to an admin role

So docs and tests should treat browser auth as stubbed, not as a full live Keycloak login flow.

### Weaviate sample data gives unrealistic search behavior

Checks:

- sample vectors loaded by `infrastructure/weaviate/load-sample-data.sh` are smoke-test data only
- sample vectors are 10-dimensional, while backend dev config declares `app.vector.dimension=384`

Use the sample data to verify the dev environment, not to benchmark real retrieval quality.

### Ollama appears down during local setup

Checks:

- Ollama is optional in the dev compose stack
- it starts only when `START_LLM=true`
- if not enabled, an unreachable Ollama endpoint does not mean the core dev stack failed

## Test failures

Useful locations:

- backend reports: `backend/build/reports/tests/`
- backend integration reports: `backend/build/reports/tests/integrationTest/`
- Gradle workflow integration reports: `build/reports/tests/integrationTest/`
- frontend coverage: `frontend/coverage/`
- aggregated reports: `build/reports/allTests/`

Example:

```bash
./gradlew integrationTest
```

If the root Gradle workflow integration tests fail, check:

- `healthCheck` output first, because `integrationTest` depends on it
- that `buildWorkspace`, `packageRelease`, and `testAllWithReport` completed and produced their expected artifacts
- that `frontend/node_modules`, `frontend/build/index.html`, `backend/build/libs/`, and `build/distributions/` exist
- whether nested Gradle execution failed before tests ran; `GradleCommandRunner` streams nested output into the test failure logs

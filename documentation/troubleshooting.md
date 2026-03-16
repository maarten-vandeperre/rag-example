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

If documents remain in `UPLOADED`, use the stuck-document admin endpoints to inspect or clean them up instead of editing rows manually:

```bash
curl http://localhost:8081/api/admin/documents/stuck
curl -X POST http://localhost:8081/api/admin/documents/stuck/<documentId>/mark-failed
curl -X POST http://localhost:8081/api/admin/documents/stuck/cleanup
```

### Search works but knowledge graph data is missing

Checks:

- this is currently a supported degraded mode in the modular document-processing pipeline
- search indexing and knowledge extraction are tracked separately
- a knowledge extraction failure does not have to block document readiness for search

Use this expectation when a document can be queried successfully but does not yet appear in knowledge graph views.

If knowledge extraction intermittently succeeds after retries, that is also expected: the modular pipeline now applies timeout and retry controls to knowledge processing without re-running vector indexing unnecessarily.

## Chat problems

### No answer found

Checks:

- confirm at least one document is `READY`
- confirm the user is allowed to query the matching documents
- for standard users, only their own `READY` documents are searched
- verify the question overlaps with extracted content

### Unable to process chat query

Checks:

- confirm the dev database schema includes the chat persistence changes required for answer references
- restart local services so the dev migration runs again if your database was created before the latest chat changes
- verify PostgreSQL is healthy before restarting the backend
- in development, send `X-User-Id` with the chat request

Example:

```bash
curl -X POST http://localhost:8081/api/chat/query \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 11111111-1111-1111-1111-111111111111' \
  -d '{"question":"How do I upload a file?","maxResponseTimeMs":20000}'
```

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

### Answer details show warnings or fail to load

Checks:

- confirm the chat answer returned an `answerId`
- retry the answer detail request once to rule out transient frontend fetch failures
- if some sources load and others do not, treat the warning as partial degradation rather than a total failure
- use browser dev tools to inspect `/api/chat/answers/{answerId}/sources` and `/api/documents/{documentId}/content`

Typical outcomes:

- no sources: the answer has no linked source records
- partial failure: some source entries are available and some are not
- retryable error: network or timeout issue while loading source detail data

If the drawer previously showed `No source snippet is available`, regenerate the answer and retry. New answers now persist answer-to-chunk references with snippet text, so source detail lookups should survive backend restarts.

## Infrastructure problems

### Compose service fails to start

Checks:

- run `podman-compose -f docker-compose.yml ps`
- inspect logs with `podman-compose -f docker-compose.yml logs <service>`
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

The service scripts now use `podman-compose`, so verify Podman health before debugging container state.

If you still have old Docker-oriented shell habits, prefer rerunning the repository helpers instead of mixing container runtimes during the same local session.

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

### Keycloak realm import or auth smoke tests fail

Checks:

- run `./infrastructure/keycloak/validate-realm.sh` before restarting services
- rerun `./infrastructure/keycloak/test-auth.sh` after Keycloak is healthy
- confirm the realm is `rag-app-dev`
- confirm the expected clients exist: `rag-app-backend` and `rag-app-frontend`
- if import still fails, inspect `keycloak-dev` container logs from `./troubleshoot-dev-services.sh`

Example recovery flow:

```bash
./stop-dev-services.sh --clean
./start-dev-services.sh
./infrastructure/keycloak/validate-realm.sh
./infrastructure/keycloak/test-auth.sh
```

### Weaviate sample data gives unrealistic search behavior

Checks:

- sample vectors loaded by `infrastructure/weaviate/load-sample-data.sh` are smoke-test data only
- sample vectors are 10-dimensional, while backend dev config declares `app.vector.dimension=384`

Use the sample data to verify the dev environment, not to benchmark real retrieval quality.

### Weaviate schema initialization fails

Symptoms:

- `./start-dev-services.sh` pauses at Weaviate initialization
- schema creation returns `422`
- `DocumentChunk` is missing from `/v1/schema`

Checks:

- run `./infrastructure/weaviate/init-weaviate-dev.sh`
- run `./infrastructure/weaviate/troubleshoot-weaviate.sh`
- confirm `infrastructure/weaviate/dev-schema.json` is valid JSON
- confirm `http://localhost:8080/v1/meta` responds before retrying schema creation

Example verification:

```bash
curl http://localhost:8080/v1/meta
curl http://localhost:8080/v1/schema
```

### Ollama appears down during local setup

Checks:

- verify `curl http://localhost:11434/api/tags` returns the local model list
- verify `tinyllama` is available before debugging backend chat quality issues
- restart `./start-dev-services.sh` if the model bootstrap did not complete cleanly

Current expectation: the dev backend targets a real local Ollama endpoint, so an unreachable Ollama service can directly affect chat behavior.

### Weaviate-backed chat retrieval is missing expected content

Checks:

- confirm the backend dev profile is using `app.vectorstore.provider=weaviate`
- verify the uploaded document created `DocumentChunk` entries in Weaviate
- if your local Weaviate schema predates the current setup, rerun `./infrastructure/weaviate/init-weaviate-dev.sh`

Example verification:

```bash
curl http://localhost:8080/v1/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ Get { DocumentChunk { documentId chunkIndex textContent fileName } } }"}'
```

### Neo4j is unavailable in native development

Checks:

- open `http://localhost:7474` and log in with `neo4j / dev-password`
- run `./infrastructure/neo4j/init-neo4j-dev.sh` if constraints were not created
- run `./infrastructure/neo4j/troubleshoot-neo4j.sh` for connectivity and recent-log output
- verify ports `7474` and `7687` are free before restarting the dev stack

## Test failures

### Backend dev mode fails on Java 25

Symptoms:

- `./gradlew :backend:quarkusDev` fails during startup
- errors mention unsupported class file versions or Gradle DSL incompatibilities

Checks:

- confirm `java -version` reports Java 25
- confirm `./gradlew --version` reports the repository wrapper version
- rerun `./gradlew :backend:test`
- if you use Maven inside `backend/`, keep it on the aligned `backend/pom.xml` instead of mixing external Quarkus versions

Example verification flow:

```bash
java -version
./gradlew --version
./gradlew :backend:test
./gradlew :backend:quarkusDev
```

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

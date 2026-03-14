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

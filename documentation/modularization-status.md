# Modularization Status

## Current state

The repository now contains extracted product-aligned modules, but the live application still runs primarily from the legacy backend and frontend runtime paths.

Legacy runtime paths:

- backend runtime: `backend/src/main/java/com/rag/app/`
- frontend runtime shell: `frontend/src/`

Extracted module paths:

- `backend/document-management`
- `backend/chat-system`
- `backend/user-management`
- `backend/shared-kernel`
- `backend/application-integration`
- `backend/integration-tests`
- `frontend/src/modules/`

## Backend modules

### Document management

Provides a standalone document area with facade operations for:

- `uploadDocument`
- `processDocument`
- `getUserDocuments`
- `getAdminProgress`

Rules carried into the module:

- maximum file size `41943040`
- supported file types remain PDF, Markdown, and plain text

Caveat: the module includes placeholder persistence pieces and is not the live API backing implementation yet.

### Chat system

Provides standalone chat and search operations for:

- `queryDocuments`
- `getChatHistory`
- `storeDocumentVectors`
- `removeDocumentVectors`
- `searchSimilarContent`

Caveat: the live `/api/chat/query` flow is still served from the legacy backend runtime.

### User management

Provides standalone user and authorization use cases around:

- profile and session behavior
- `STANDARD` vs `ADMIN`
- role-management flows

Caveat: this is not a full live Keycloak browser integration layer.

### Shared kernel

Introduces shared concepts including:

- `EntityId`
- `Timestamp`
- `Email`
- `FileSize`
- `DomainEvent`
- `ValidationException`

Caveat: the shared kernel is present, but adoption is still partial across modules.

### Application integration layer

Provides cross-module orchestration through:

- `ApplicationOrchestrator`
- event bus abstractions
- module-health snapshots
- integration events for upload, processing, auth, and query flows

Caveat: this layer is scaffolding and orchestration code, not the current Quarkus runtime path.

### Module integration tests

The separate `backend/integration-tests` module validates cross-module behavior with fixtures and stubs.

Run:

```bash
./gradlew :backend:integration-tests:test
```

These tests cover:

- boundary checks
- workflow checks
- event checks
- module health checks
- compatibility checks

Caveat: these are cross-module tests, not full HTTP/database integration tests.

## Frontend modularization

Frontend feature folders now exist under `frontend/src/modules/`, with route-level composition and lint rules for module boundaries.

Supporting pieces include:

- `frontend/src/routes/AppRoutes.js`
- `frontend/eslint-rules/no-cross-module-imports.js`
- `npm run lint:all`

Caveat: much of the module structure still wraps legacy `components/` and `services/`, so the separation is partial rather than complete.

## How to interpret the repo today

- the new modules document the intended long-term boundaries
- the legacy runtime remains the main execution path
- some adapters in the extracted modules are intentionally placeholders
- architecture and onboarding docs should describe both the target modular shape and the current runtime reality

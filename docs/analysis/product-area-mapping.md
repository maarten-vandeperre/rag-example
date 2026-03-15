# Product Area Mapping

## Target Product Areas

This workspace naturally groups into five product areas:

1. Document management
2. Chat system
3. User management
4. Shared kernel
5. Platform and infrastructure

## Current To Proposed Mapping

| Product area | Current backend locations | Current frontend locations | Notes |
| --- | --- | --- | --- |
| Document management | `api/DocumentUploadController`, `usecases/UploadDocument`, `usecases/ProcessDocument`, `infrastructure/document`, `infrastructure/persistence/JdbcDocumentRepository` | `components/DocumentLibrary`, part of `services/DocumentApiClient.js` | Strong candidate for first extraction because the workflow is already coherent |
| Chat system | `api/ChatController`, `usecases/QueryDocuments`, `infrastructure/llm`, `infrastructure/vector` | `components/ChatWorkspace`, `services/ChatApiClient.js` | Depends on document search access and shared references |
| User management | `domain/entities/User`, `domain/valueobjects/UserRole`, `infrastructure/persistence/JdbcUserRepository`, role checks in multiple use cases | role selection in `App.js`, request identity passed through service calls | Currently scattered and mostly implicit rather than feature-owned |
| Admin operations | `api/DocumentLibraryResource`, `usecases/GetAdminProgress`, reporting models in `usecases.models` | `components/AdminProgress` | Operationally separate from end-user document library, but shares document data access |
| Shared kernel | `domain/valueobjects/DocumentReference`, `DocumentStatus`, `FileType`; shared input/output concepts | `services/ApiClient.js`, `utils/HttpClient.js` | Not formalized yet; concepts are reused across nearly every area |

## Backend Package Reorganization Candidate

### Proposed feature-aligned backend slices

- `document-management`
  - upload, processing, document repository adapters, extraction
- `chat-system`
  - query orchestration, vector search, answer generation
- `user-management`
  - user repository, authorization services, identity policies
- `shared-kernel`
  - document reference, common enums, cross-feature policies
- `platform-infrastructure`
  - framework bootstrapping, JDBC support, configuration

### Why the current structure resists this split

- Technical layers hide product ownership.
- `DocumentLibraryResource` combines user document listing and admin reporting under one resource.
- Chat depends on document and user access through generic repositories instead of feature-specific service boundaries.

## Frontend Package Reorganization Candidate

### Proposed feature-aligned frontend slices

- `document-management`
  - upload form, document list, document API integration
- `chat-workspace`
  - chat page, question input, answer rendering, chat API integration
- `user-management`
  - current-user context, role-aware navigation, admin gating
- `shared-components`
  - generic UI building blocks used across feature areas
- `core`
  - app shell, environment configuration, shared HTTP abstraction

## Missing Boundaries Today

- No dedicated authorization service in backend or frontend.
- No explicit boundary between admin operations and end-user document browsing.
- No shared kernel package for concepts used by both chat and document features.
- No frontend core layer that owns service composition and user context.

## Migration Strategy Outline

1. Establish a shared kernel for cross-feature domain concepts.
2. Extract user-management policies so authorization stops being duplicated across use cases.
3. Split document-management from admin-operations concerns.
4. Move chat-specific infrastructure under a chat-system feature boundary.
5. Reorganize frontend around feature folders plus a small `core` and `shared-components` layer.

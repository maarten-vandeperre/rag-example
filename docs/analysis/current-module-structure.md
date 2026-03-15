# Current Module Structure Analysis

## Workspace Baseline

- Root Gradle build in `build.gradle` orchestrates two modules declared in `settings.gradle`: `backend` and `frontend`.
- The workspace is still organized as technical layers rather than product-aligned modules.
- Shared operational concerns such as health checks, workspace packaging, and aggregated test reporting live in the root build instead of dedicated platform modules.

## Backend Structure

Backend code lives under `backend/src/main/java/com/rag/app/` and is grouped into four top-level areas:

| Area | Current packages | Primary responsibility |
| --- | --- | --- |
| API | `api`, `api.dto` | REST entrypoints, DTO mapping, transport validation |
| Domain | `domain.entities`, `domain.valueobjects` | Core business state such as documents, users, chat messages, and document references |
| Use cases | `usecases`, `usecases.models`, `usecases.interfaces`, `usecases.repositories` | Application workflows, input/output models, ports |
| Infrastructure | `infrastructure.document`, `infrastructure.llm`, `infrastructure.persistence`, `infrastructure.vector` | Technical adapters for storage, extraction, vector search, and answer generation |

## Backend Functionality Mapping

### Document management

- Upload entrypoint: `backend/src/main/java/com/rag/app/api/DocumentUploadController.java`
- Upload workflow: `backend/src/main/java/com/rag/app/usecases/UploadDocument.java`
- Processing workflow: `backend/src/main/java/com/rag/app/usecases/ProcessDocument.java`
- Persistence: `backend/src/main/java/com/rag/app/infrastructure/persistence/JdbcDocumentRepository.java`
- Extraction: `backend/src/main/java/com/rag/app/infrastructure/document/DocumentContentExtractorImpl.java`

### Chat and query system

- Query endpoint: `backend/src/main/java/com/rag/app/api/ChatController.java`
- Query workflow: `backend/src/main/java/com/rag/app/usecases/QueryDocuments.java`
- Answer generation: `backend/src/main/java/com/rag/app/infrastructure/llm/AnswerGeneratorImpl.java`
- Vector storage and search: `backend/src/main/java/com/rag/app/infrastructure/vector/VectorStoreImpl.java`

### User management and authorization

- Core user state: `backend/src/main/java/com/rag/app/domain/entities/User.java`
- Roles: `backend/src/main/java/com/rag/app/domain/valueobjects/UserRole.java`
- User access checks are embedded in `UploadDocument`, `GetUserDocuments`, `GetAdminProgress`, and `QueryDocuments` rather than centralized.

### Operational reporting

- Admin endpoint: `backend/src/main/java/com/rag/app/api/DocumentLibraryResource.java`
- Admin workflow: `backend/src/main/java/com/rag/app/usecases/GetAdminProgress.java`
- Reporting models currently live beside general use case models in `usecases.models`.

## Frontend Structure

Frontend code lives under `frontend/src/` and is arranged by UI type more than product boundary:

| Area | Current paths | Responsibility |
| --- | --- | --- |
| App shell | `App.js`, `App.css`, `index.js` | Bootstrapping and role-based entry selection |
| Feature components | `components/DocumentLibrary`, `components/ChatWorkspace`, `components/AdminProgress` | Product-facing UI |
| Service layer | `services/ApiClient.js`, `services/DocumentApiClient.js`, `services/ChatApiClient.js`, `services/ErrorHandler.js` | API orchestration and error mapping |
| Shared utility | `utils/HttpClient.js` | Shared fetch/upload behavior |

## Shared Components And Utilities

- `frontend/src/services/ApiClient.js` acts as a composition point for document and chat services, making it a de facto frontend core service.
- `frontend/src/utils/HttpClient.js` is reused by document and chat clients, but it depends on `services/ErrorHandler.js`, which creates a reverse utility-to-service dependency.
- Backend shared concepts such as `DocumentReference`, `DocumentStatus`, `FileType`, and `UserRole` are reused across multiple product areas, but they are not yet separated into a shared kernel module.

## Structural Observations

- Product behaviors are spread across `api`, `usecases`, and `infrastructure`, so a single feature requires navigation across multiple technical directories.
- Admin progress currently shares the same API resource and use case model space as document listing, which weakens module boundaries.
- Chat, document management, and user authorization all depend on the same central domain package tree, making future extraction into feature modules non-trivial.

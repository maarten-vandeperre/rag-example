# Dependency Analysis

## Current Dependency Direction

The backend mostly follows a clean architecture flow:

`api -> usecases -> domain`

and

`infrastructure -> usecases/domain`

Observed examples:

- `DocumentUploadController` imports domain value objects directly for file validation.
- `QueryDocuments` depends on domain entities and value objects plus use case ports.
- `AnswerGeneratorImpl` and `VectorStoreImpl` implement use case interfaces from infrastructure.
- `JdbcDocumentRepository` implements repository ports and also returns use case reporting models.

## Backend Coupling Findings

### 1. API layer reaches into domain details

- `backend/src/main/java/com/rag/app/api/DocumentUploadController.java` imports `DocumentMetadata` and `FileType`.
- This means transport validation knows about domain storage limits and type rules instead of going through an application-facing contract.

### 2. Reporting models are mixed with core application models

- `JdbcDocumentRepository` returns `FailedDocumentInfo`, `ProcessingDocumentInfo`, and `ProcessingStatistics` from `usecases.models`.
- Those models serve admin reporting, but they are stored in the same package family as upload/query/process models.

### 3. User authorization is duplicated across use cases

- `UploadDocument`, `GetUserDocuments`, `GetAdminProgress`, and `QueryDocuments` each resolve users and roles directly.
- This creates repeated access-control logic and couples multiple product areas to the same user repository behavior.

### 4. Chat depends on document management and shared user rules

- `QueryDocuments` loads accessible documents through `DocumentRepository` and role checks through `UserRepository`.
- Chat behavior therefore cannot be extracted cleanly without a dedicated boundary for document search access.

### 5. Infrastructure owns feature-specific branching

- `DocumentContentExtractorImpl`, `VectorStoreImpl`, and `AnswerGeneratorImpl` each encode workflow assumptions for a specific product area.
- Those adapters are placed in technical buckets, which hides that they belong to document-management or chat-system capabilities.

## Frontend Coupling Findings

### 1. App shell chooses features by role

- `frontend/src/App.js` imports `AdminProgress` and `DocumentLibrary` directly and switches on environment-driven role state.
- This keeps feature composition centralized but also makes `App.js` responsible for product routing decisions.

### 2. Components depend on a shared aggregate API client

- `DocumentLibrary.jsx` and `ChatWorkspace.jsx` both instantiate `ApiClient` directly.
- This hides product-specific boundaries behind one general client and makes component tests rely on the same shared abstraction.

### 3. Utility-service dependency is inverted

- `frontend/src/utils/HttpClient.js` imports error mapping from `frontend/src/services/ErrorHandler.js`.
- Utilities should normally be leaf dependencies, but here the utility depends on service-level behavior.

### 4. Admin functionality remains co-located with document flows

- Admin progress UI is a distinct feature, yet it remains under the shared `components/` tree with no separate application core for authorization or navigation.

## Circular Dependency Review

- No explicit package-level circular dependencies were found from the inspected imports.
- The main risk is conceptual circularity: shared domain concepts are used by document, chat, and admin flows without feature ownership, which will make future modularization harder even before import cycles appear.

## Dependency Graph Snapshot

### Backend

- `api`
  - depends on `usecases`
  - sometimes depends on `domain.valueobjects`
- `usecases`
  - depends on `domain`
  - depends on `usecases.interfaces` and `usecases.repositories`
- `infrastructure`
  - depends on `usecases.interfaces`, `usecases.repositories`, `usecases.models`
  - depends on `domain`

### Frontend

- `App`
  - depends on feature components
- feature components
  - depend on `services/ApiClient`
- `services/*ApiClient`
  - depend on `utils/HttpClient`
- `utils/HttpClient`
  - depends on `services/ErrorHandler`

## Refactoring Pressure Points

- API-to-domain validation shortcuts in upload and chat endpoints.
- Shared role checks repeated across use cases.
- Admin progress types mixed into generic use case model packages.
- Frontend utility and service layering inversion around `HttpClient` and `ErrorHandler`.

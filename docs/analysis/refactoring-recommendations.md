# Refactoring Recommendations

## High-Value Improvements

### 1. Introduce explicit product modules

- Create backend modules for document management, chat system, user management, and shared kernel.
- Keep infrastructure adapters close to their owning feature unless they are truly cross-cutting.
- Mirror that split in the frontend so UI and API client code move together.

### 2. Centralize authorization and identity rules

- Extract repeated role and user checks from `UploadDocument`, `GetUserDocuments`, `GetAdminProgress`, and `QueryDocuments` into a dedicated user-management service or policy boundary.
- This reduces duplication and gives future modules a single integration point.

### 3. Remove API-to-domain validation shortcuts

- Move file size and file type decision logic behind an application contract instead of having `DocumentUploadController` import domain value objects directly.
- Keep REST resources focused on transport concerns.

### 4. Separate admin operations from document browsing

- Split `DocumentLibraryResource` into distinct user document and admin operations endpoints.
- Move reporting DTOs and models under an admin-oriented namespace so the intent is clear.

### 5. Create a shared kernel with strict ownership

- Start with concepts already reused across multiple areas: `DocumentReference`, `DocumentStatus`, `FileType`, and maybe common identifiers.
- Avoid placing feature-specific reporting or transport models in the shared kernel.

### 6. Simplify frontend layering

- Move `ApiClient` composition into a frontend `core` layer.
- Keep feature components dependent on feature-specific hooks or clients rather than a single catch-all client.
- Untangle `utils/HttpClient.js` from `services/ErrorHandler.js` so utilities remain foundational.

## Recommended Execution Order

1. Define shared-kernel ownership rules.
2. Extract user-management policies and identity access.
3. Split document-management and admin-operations boundaries.
4. Extract chat-system infrastructure and orchestration.
5. Reorganize frontend folders to match backend feature areas.

## Feasibility Notes

- The current codebase is small enough that reorganization is practical without introducing compatibility layers for long.
- Existing tests already cover upload, processing, query, and admin flows, so they provide a safety net for structural changes.
- Root Gradle workspace automation can continue unchanged while product modules are introduced incrementally.

## Risks To Watch

- Over-eager shared-kernel extraction can create a new dumping ground.
- Chat extraction may stall if document visibility and user authorization stay coupled to generic repositories.
- Frontend folder moves without service boundary cleanup would only rename directories, not improve maintainability.

## Definition Of Done For The Next Refactor Phase

- Each product area owns its API, application, and adapter code.
- Shared concepts are minimal and stable.
- Authorization rules live in one place.
- Frontend feature folders map cleanly to backend product modules.
- Cross-feature dependencies are intentional and documented.

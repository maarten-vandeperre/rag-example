# Architecture

## Layered structure

The backend follows a clean-architecture-style separation inside `backend/src/main/java/com/rag/app/`:

- `domain/` for entities and value objects
- `usecases/` for application workflows
- `api/` for REST resources and DTOs
- `infrastructure/` for persistence, extraction, vector, and answer-generation adapters

## Domain model

### Document

`Document` represents an uploaded file with:

- metadata in `DocumentMetadata`
- type in `FileType`: `PDF`, `MARKDOWN`, `PLAIN_TEXT`
- lifecycle in `DocumentStatus`: `UPLOADED`, `PROCESSING`, `READY`, `FAILED`
- a required `contentHash` used for duplicate detection

Hard rule: document metadata enforces the 40 MB limit (`41943040` bytes).

### User

`User` includes:

- UUID identity
- username and email
- role in `UserRole`: `STANDARD` or `ADMIN`
- active/inactive state

Use cases check that uploads come from active users.

### Chat message

`ChatMessage` stores:

- a required question
- answer text
- response time in milliseconds
- zero or more source references

References use `DocumentReference`, which points back to the source document and paragraph metadata.

### Knowledge graph domain primitives

The shared kernel now includes a knowledge graph domain model for extracted document knowledge:

- `KnowledgeGraph` as the aggregate root
- `KnowledgeNode` for extracted entities and concepts
- `KnowledgeRelationship` for links between nodes
- value objects such as `GraphId`, `NodeId`, `RelationshipId`, `NodeType`, `RelationshipType`, and `ConfidenceScore`
- provenance metadata such as `DocumentReference`, `GraphMetadata`, and `ExtractionMetadata`

These types are immutable, validation-heavy, and designed to be reused across extraction, persistence, and administration flows.

## Backend workflow

### Upload flow

1. `DocumentUploadController` validates multipart input
2. `UploadDocument` validates the user and file content
3. the document is persisted through `DocumentRepository`
4. duplicate detection uses a SHA-256 content hash

### Processing flow

1. `ProcessDocument` moves a document from `UPLOADED` to `PROCESSING`
2. `DocumentContentExtractorImpl` extracts text from PDF, Markdown, or plain text
3. blank or unreadable content marks the document as `FAILED`
4. successful extraction stores chunks in the vector adapter
5. development mode uses a real `WeaviateVectorStore` when `app.vectorstore.provider=weaviate`
6. knowledge extraction can run alongside search indexing with its own status tracking
7. successful uploads attempt Neo4j-backed knowledge graph creation for supported document types
8. search readiness is preserved even when knowledge extraction fails
9. the document is still allowed to reach `READY` for search when core processing succeeds

Current extraction limits:

- PDF extraction uses PDFBox
- Markdown extraction strips markdown syntax into readable text
- plain text supports UTF-8 and UTF-16 BOM handling
- OCR is not implemented
- password-protected PDFs are unsupported
- extracted text must contain at least 10 readable characters

Knowledge-processing defaults currently use a shared-kernel configuration with:

- default graph name `main-knowledge-graph`
- document-type specific extraction options for `PDF`, `MARKDOWN`, and `PLAIN_TEXT`
- a default processing timeout of 10 seconds
- a single retry attempt for transient extraction failures

The current runtime extraction implementation is heuristic rather than model-backed. It performs English-focused section, entity, concept, and relationship extraction with confidence filtering and chunk-aware processing.

### Query flow

1. `ChatController` validates the request and timeout
2. `QueryDocuments` selects `READY` documents visible to the caller
3. semantic search returns the top 5 chunks above the relevance threshold
4. in development, the backend can call Ollama through `OllamaLlmClient` when `app.llm.provider=ollama`
5. the answer generator creates a grounded answer with source references
6. the backend persists the answer and its answer-to-chunk source references together
7. the API returns an `answerId` only after persistence succeeds, so later source-detail lookups have durable backing data

### Answer detail retrieval flow

1. the frontend stores the `answerId` from `POST /api/chat/query`
2. `AnswerSourceController` loads source metadata for that answer and enforces ownership checks
3. `GetAnswerSourceDetails` reads persisted `answer_source_references` rows and returns stored snippet text plus availability flags for the UI
4. `DocumentContentController` returns full document content when the user opens the document viewer

Current implementation notes:

- development configuration now targets real local services for vector search, graph persistence, and LLM responses
- local fallbacks still exist for some failure cases so runtime startup is more resilient during development

## Persistence

PostgreSQL schema in `backend/src/main/resources/schema.sql` stores:

- `users`
- `documents`
- `chat_messages`
- `document_references`
- `answer_source_references`

JDBC adapters live in `backend/src/main/java/com/rag/app/infrastructure/persistence/`.

Important persistence details:

- document status updates clear `failure_reason` when appropriate
- admin reporting reads from `last_updated`, `failure_reason`, and `processing_started_at`
- chat references are saved transactionally with ordered `reference_index`
- answer detail retrieval uses durable answer-to-chunk references with stored snippet text, relevance score, ordering, and cached document metadata

## Infrastructure services

### PostgreSQL

- runs from `infrastructure/database`
- includes pgvector support
- persists state in the `postgres-data` volume

### Weaviate

- bootstrapped by `weaviate-init`
- persists state in `weaviate-data`
- defines a `DocumentChunk` schema for chunk search metadata
- dev startup can add missing schema properties automatically so document indexing does not fail on older local data

### Ollama

- bootstrapped by `ollama-init`
- keeps pulled models in `ollama-models`
- defaults to `tinyllama`
- development mode now targets a real Ollama endpoint with configurable timeout, retry count, temperature, and max-token settings

### Neo4j

- available in the native development stack for graph-backed experimentation and future graph persistence work
- exposed locally through Neo4j Browser on `7474` and Bolt on `7687`
- bootstrapped with scripts under `infrastructure/neo4j/` for constraints, sample graph data, and troubleshooting
- shared-kernel knowledge graph persistence now has a Neo4j repository adapter with mapper-based domain reconstruction and reusable Cypher query helpers

## Frontend structure

Frontend code lives in `frontend/src/`:

- `components/DocumentLibrary/` for upload and document list flows
- `components/ChatWorkspace/` for question and answer flows
- `components/AdminProgress/` for operations monitoring
- `services/` for API clients and error handling
- `utils/HttpClient.js` for shared request behavior

## Modularization status

The repository now contains extracted backend and frontend module scaffolding in addition to the legacy runtime.

Backend extracted modules under `backend/`:

- `document-management`
- `chat-system`
- `user-management`
- `shared-kernel`
- `application-integration`
- `integration-tests`

Frontend extracted feature structure under `frontend/src/modules/`:

- feature folders and route-level wrappers
- lint rules that discourage cross-module imports
- lazy routing via `frontend/src/routes/AppRoutes.js`

Important caveats:

- the live Quarkus API still primarily runs from `backend/src/main/java/com/rag/app/`
- the extracted backend modules are not the production runtime path yet
- some adapters inside the new modules are placeholders, especially JDBC integrations
- frontend module boundaries are partial and still wrap legacy `components/` and `services/`

## New backend module responsibilities

### `backend/document-management`

- owns document upload, processing, document listing, and admin progress facades
- keeps domain rules like supported file types and the `41943040` byte file-size limit
- uses in-memory doubles in tests

Current caveat: the placeholder JDBC repository in this module is not wired into the live backend.

### `backend/chat-system`

- owns query, chat history, vector storage, and semantic search abstractions
- includes facade methods such as `queryDocuments`, `getChatHistory`, `storeDocumentVectors`, `removeDocumentVectors`, and `searchSimilarContent`

Current caveat: this module is not the production `/api/chat/query` runtime path yet.

### `backend/user-management`

- centralizes user, role, and authorization use cases around `STANDARD` and `ADMIN`
- includes simple auth and session-oriented test behavior
- knowledge graph administration now uses the real CDI-wired user-management facade and JDBC-backed user repository instead of a permissive stub

Current caveat: real Keycloak-backed frontend/browser authentication is not implemented through this module.

### `backend/shared-kernel`

- provides shared concepts such as `EntityId`, `Timestamp`, `Email`, `FileSize`, `DomainEvent`, and `ValidationException`

Current caveat: the shared kernel exists, but adoption across all feature modules is still incomplete.

### `backend/application-integration`

- contains `ApplicationOrchestrator`, event bus abstractions, integration events, and plain Java controllers for cross-module coordination
- publishes richer document-processing events that include knowledge-processing outcomes for downstream observers

Current caveat: this layer is scaffolding and orchestration code, not Quarkus-wired runtime integration.

### `backend/integration-tests`

- provides cross-module compatibility and workflow tests
- runs with fixtures and stubs rather than full HTTP/database runtime integration

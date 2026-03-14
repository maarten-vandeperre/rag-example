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
4. successful extraction stores chunks in the vector adapter and marks the document `READY`

Current extraction limits:

- PDF extraction uses PDFBox
- Markdown extraction strips markdown syntax into readable text
- plain text supports UTF-8 and UTF-16 BOM handling
- OCR is not implemented
- password-protected PDFs are unsupported
- extracted text must contain at least 10 readable characters

### Query flow

1. `ChatController` validates the request and timeout
2. `QueryDocuments` selects `READY` documents visible to the caller
3. semantic search returns the top 5 chunks above the relevance threshold
4. the answer generator creates a grounded answer with source references
5. chat history is persisted through `ChatMessageRepository`

Current implementation caveat:

- the compose stack provisions Weaviate and Ollama
- the application currently uses `VectorStoreImpl` as an in-memory semantic store and `HeuristicLlmClient` for answer generation

## Persistence

PostgreSQL schema in `backend/src/main/resources/schema.sql` stores:

- `users`
- `documents`
- `chat_messages`
- `document_references`

JDBC adapters live in `backend/src/main/java/com/rag/app/infrastructure/persistence/`.

Important persistence details:

- document status updates clear `failure_reason` when appropriate
- admin reporting reads from `last_updated`, `failure_reason`, and `processing_started_at`
- chat references are saved transactionally with ordered `reference_index`

## Infrastructure services

### PostgreSQL

- runs from `infrastructure/database`
- includes pgvector support
- persists state in the `postgres-data` volume

### Weaviate

- bootstrapped by `weaviate-init`
- persists state in `weaviate-data`
- defines a `DocumentChunk` schema for chunk search metadata

### Ollama

- bootstrapped by `ollama-init`
- keeps pulled models in `ollama-models`
- defaults to `tinyllama`

## Frontend structure

Frontend code lives in `frontend/src/`:

- `components/DocumentLibrary/` for upload and document list flows
- `components/ChatWorkspace/` for question and answer flows
- `components/AdminProgress/` for operations monitoring
- `services/` for API clients and error handling
- `utils/HttpClient.js` for shared request behavior

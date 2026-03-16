# API Documentation

## Authentication and identity today

The current API uses a temporary mixed identity model:

- document list and admin progress expect `X-User-Id`
- upload expects a multipart `userId` field
- chat expects an authenticated principal UUID in `SecurityContext`

Document this clearly in integrations because the identity contract is not yet unified.

## Core rules

- supported files: `.pdf`, `.md`, `.txt`
- maximum upload size: `41943040` bytes
- only `READY` documents can be queried
- extracted document text must contain at least 10 readable characters
- default chat timeout: `20000` ms

## `GET /api/health`

- Method: `GET`
- Endpoint: `/api/health`
- Request body: none
- Response body:

```json
{
  "status": "ok"
}
```

Example:

```bash
curl http://localhost:8080/api/health
```

## `POST /api/documents/upload`

- Method: `POST`
- Endpoint: `/api/documents/upload`
- Content type: `multipart/form-data`

Required form fields:

- `file`
- `userId` as a UUID string

Example:

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F 'userId=11111111-1111-1111-1111-111111111111' \
  -F 'file=@./sample.md;type=text/markdown'
```

Success response (`201`):

```json
{
  "documentId": "8f2fdd84-dc1d-4b56-b322-6c53c8f1e3bf",
  "fileName": "sample.md",
  "status": "UPLOADED",
  "message": "Document uploaded successfully",
  "uploadedAt": "2026-03-14T10:15:30Z"
}
```

Validation and error responses:

- `400` invalid request or invalid UUID
- `413` file too large
- `415` unsupported file type
- `500` upload or read failure

Example error body:

```json
{
  "error": "UNSUPPORTED_FILE_TYPE",
  "message": "Only PDF, Markdown, and plain text files are supported",
  "timestamp": "2026-03-14T10:15:30Z"
}
```

## `GET /api/documents`

- Method: `GET`
- Endpoint: `/api/documents`
- Required header: `X-User-Id`
- Query parameter: `includeAll=false` by default

Behavior:

- standard users receive only their own documents
- admins can request all documents with `includeAll=true`
- results are returned newest first

Example:

```bash
curl 'http://localhost:8080/api/documents?includeAll=false' \
  -H 'X-User-Id: 11111111-1111-1111-1111-111111111111'
```

Response body:

```json
{
  "documents": [
    {
      "documentId": "8f2fdd84-dc1d-4b56-b322-6c53c8f1e3bf",
      "fileName": "sample.md",
      "fileSize": 1024,
      "fileType": "MARKDOWN",
      "status": "READY",
      "uploadedBy": "11111111-1111-1111-1111-111111111111",
      "uploadedAt": "2026-03-14T10:15:30Z",
      "lastUpdated": "2026-03-14T10:15:30Z"
    }
  ],
  "totalCount": 1
}
```

Validation errors:

- malformed or missing `X-User-Id` currently falls through as a server error response from the resource

## `GET /api/admin/documents/progress`

- Method: `GET`
- Endpoint: `/api/admin/documents/progress`
- Required header: `X-User-Id`

Behavior:

- admin only
- returns aggregate counts, failed documents, and in-flight processing documents
- failed documents are sorted newest upload first
- processing documents are sorted by oldest `processingStartedAt`

Example:

```bash
curl http://localhost:8080/api/admin/documents/progress \
  -H 'X-User-Id: 22222222-2222-2222-2222-222222222222'
```

Response body:

```json
{
  "statistics": {
    "totalDocuments": 12,
    "uploadedCount": 1,
    "processingCount": 2,
    "readyCount": 8,
    "failedCount": 1
  },
  "failedDocuments": [
    {
      "documentId": "6be5d4f8-2c4a-46b9-a65d-b922d85b6357",
      "fileName": "broken.pdf",
      "uploadedBy": "11111111-1111-1111-1111-111111111111",
      "uploadedAt": "2026-03-14T10:15:30Z",
      "failureReason": "Extracted document content was blank",
      "fileSize": 2048
    }
  ],
  "processingDocuments": [
    {
      "documentId": "c62884a9-2f59-45fe-8453-2e10e1f89fb4",
      "fileName": "manual.pdf",
      "uploadedBy": "11111111-1111-1111-1111-111111111111",
      "uploadedAt": "2026-03-14T10:10:00Z",
      "processingStartedAt": "2026-03-14T10:10:05Z"
    }
  ]
}
```

Validation errors:

- `403` for non-admin or invalid admin access
- `500` for unexpected failures

## `POST /api/chat/query`

- Method: `POST`
- Endpoint: `/api/chat/query`
- Content type: `application/json`
- Auth requirement: authenticated principal name must be a UUID

Request body:

```json
{
  "question": "What is the retention policy?",
  "maxResponseTimeMs": 20000
}
```

Example:

```bash
curl -X POST http://localhost:8080/api/chat/query \
  -H 'Content-Type: application/json' \
  -d '{"question":"What is the retention policy?","maxResponseTimeMs":20000}'
```

Success response (`200`):

```json
{
  "answerId": "51f0a2dd-3e79-4f53-9c7d-0c3d3f1cb0b7",
  "answer": "The retention policy is 30 days for temporary uploads.",
  "documentReferences": [
    {
      "documentId": "8f2fdd84-dc1d-4b56-b322-6c53c8f1e3bf",
      "documentName": "operations.txt",
      "paragraphReference": "paragraph 2",
      "relevanceScore": 0.84
    }
  ],
  "responseTimeMs": 187,
  "success": true,
  "errorMessage": null
}
```

The returned `answerId` is used by the answer detail flow to load source snippets later.

Current persistence rule:

- newly returned answers are only exposed after the backend persists the answer record and its answer-to-source references together

Error behavior:

- `400` null body, blank `question`, invalid timeout, or missing authenticated UUID
- `404` no relevant documents or no ready documents
- `408` query timeout
- `500` unexpected processing failure

No-answer response example:

```json
{
  "answerId": null,
  "answer": null,
  "documentReferences": [],
  "responseTimeMs": 42,
  "success": false,
  "errorMessage": "no answer found"
}
```

## `GET /api/chat/answers/{answerId}/sources`

- Method: `GET`
- Endpoint: `/api/chat/answers/{answerId}/sources`
- Request body: none
- Auth requirement: authenticated principal UUID

Behavior:

- validates that the answer exists and belongs to the caller
- returns answer-scoped source metadata for the selected chat answer
- includes `available` flags so the UI can degrade gracefully when a source cannot be loaded
- source details are reconstructed from persisted answer-to-chunk records, so lookups continue to work after backend restarts
- the service returns the actual stored snippet text when it is available instead of an empty snippet placeholder

Example:

```bash
curl http://localhost:8080/api/chat/answers/51f0a2dd-3e79-4f53-9c7d-0c3d3f1cb0b7/sources \
  -H 'Authorization: Bearer <token>'
```

Success response (`200`):

```json
{
  "answerId": "51f0a2dd-3e79-4f53-9c7d-0c3d3f1cb0b7",
  "sources": [
    {
      "sourceId": "chunk-001",
      "documentId": "8f2fdd84-dc1d-4b56-b322-6c53c8f1e3bf",
      "fileName": "operations.txt",
      "fileType": "PLAIN_TEXT",
      "snippet": {
        "content": "Temporary uploads are removed after 30 days.",
        "startPosition": 0,
        "endPosition": 44,
        "context": null
      },
      "metadata": {
        "title": "Operations Handbook",
        "author": null,
        "createdAt": "2026-03-16T09:00:00Z",
        "pageNumber": null,
        "chunkIndex": 0
      },
      "relevanceScore": 0.84,
      "available": true
    }
  ],
  "totalSources": 1,
  "availableSources": 1
}
```

Validation and error responses:

- `401` missing or invalid authenticated principal
- `403` answer exists but belongs to another user
- `404` answer not found
- `500` unexpected source-detail retrieval failure

Example error body:

```json
{
  "error": "Answer not found"
}
```

## `GET /api/documents/{documentId}/content`

- Method: `GET`
- Endpoint: `/api/documents/{documentId}/content`
- Request body: none
- Auth requirement: authenticated principal UUID

Behavior:

- validates that the caller can access the requested document
- returns the full stored content for text, markdown, or extracted PDF text
- used by the chat document viewer launched from answer details

Example:

```bash
curl http://localhost:8080/api/documents/8f2fdd84-dc1d-4b56-b322-6c53c8f1e3bf/content \
  -H 'Authorization: Bearer <token>'
```

Success response (`200`):

```json
{
  "documentId": "8f2fdd84-dc1d-4b56-b322-6c53c8f1e3bf",
  "fileName": "operations.txt",
  "fileType": "PLAIN_TEXT",
  "content": "Temporary uploads are removed after 30 days. Permanent records remain available to administrators.",
  "metadata": {
    "title": "Operations Handbook",
    "author": null,
    "createdAt": "2026-03-16T09:00:00Z",
    "fileSize": 1024,
    "pageCount": null
  },
  "available": true
}
```

Validation and error responses:

- `401` missing or invalid authenticated principal
- `403` document exists but the caller cannot access it
- `404` document not found
- `500` unexpected document-content retrieval failure

# Create Document Upload REST controller

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create REST controller for document upload functionality with proper file handling, validation, and error responses.

## Scope

- Create REST controller for POST /api/documents/upload
- Implement file upload handling with multipart form data
- Add request validation and error handling
- Map between REST DTOs and use case models

## Out of Scope

- File storage implementation
- Authentication/authorization middleware
- Rate limiting
- Progress tracking for large uploads

## Clean Architecture Placement

interface adapters

## Execution Dependencies

- 0004-upload_documents_and_chat_with_private_knowledge_base-create_upload_document_usecase.md

## Implementation Details

Create DocumentUploadController with:
- POST /api/documents/upload endpoint
- Accept multipart file upload
- Extract file metadata (name, size, type)
- Validate file size and type before processing
- Call UploadDocument use case
- Return appropriate HTTP status codes

Create UploadDocumentRequest DTO with:
- file (MultipartFile)
- userId (from authentication context)

Create UploadDocumentResponse DTO with:
- documentId (string)
- fileName (string)
- status (string)
- message (string)
- uploadedAt (timestamp)

HTTP status codes:
- 201 Created: successful upload
- 400 Bad Request: invalid file or validation error
- 413 Payload Too Large: file exceeds 40MB
- 415 Unsupported Media Type: unsupported file type
- 500 Internal Server Error: processing error

Error response format:
```json
{
  "error": "FILE_TOO_LARGE",
  "message": "File size exceeds maximum allowed size of 40MB",
  "timestamp": "2026-03-13T10:30:00Z"
}
```

## Files / Modules Impacted

- backend/adapters/rest/DocumentUploadController.java
- backend/adapters/rest/dto/UploadDocumentRequest.java
- backend/adapters/rest/dto/UploadDocumentResponse.java
- backend/adapters/rest/dto/ErrorResponse.java

## Acceptance Criteria

Given a valid file upload request
When POST /api/documents/upload is called
Then the file should be accepted and document created

Given a file larger than 40MB is uploaded
When POST /api/documents/upload is called
Then HTTP 413 should be returned with appropriate error message

Given an unsupported file type is uploaded
When POST /api/documents/upload is called
Then HTTP 415 should be returned with appropriate error message

Given a malformed request is sent
When POST /api/documents/upload is called
Then HTTP 400 should be returned with validation errors

## Testing Requirements

- Unit tests for DocumentUploadController
- Integration tests for file upload endpoint
- Tests for file size validation
- Tests for file type validation
- Tests for error response formats

## Dependencies / Preconditions

- UploadDocument use case must be implemented
- Quarkus REST framework must be configured
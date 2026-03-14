# Create UploadDocument use case

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create the UploadDocument use case that handles document upload validation, file size checking, and initial document creation.

## Scope

- Create UploadDocument use case with input/output models
- Implement file size validation (40MB limit)
- Implement file type validation (PDF, Markdown, plain text)
- Define repository interface for document persistence

## Out of Scope

- Actual file storage implementation
- Document content processing
- REST endpoint implementation
- Frontend upload components

## Clean Architecture Placement

usecases

## Execution Dependencies

- 0007-upload_documents_and_chat_with_private_knowledge_base-create_document_domain_entity.md
- 0008-upload_documents_and_chat_with_private_knowledge_base-create_user_domain_entity.md

## Implementation Details

Create UploadDocumentInput with:
- fileName (string)
- fileSize (long, in bytes)
- fileType (FileType enum)
- fileContent (byte array or stream reference)
- uploadedBy (userId)

Create UploadDocumentOutput with:
- documentId (generated unique identifier)
- status (DocumentStatus)
- message (success/error message)

Create DocumentRepository interface with:
- save(Document document) method
- findByContentHash(String hash) method for deduplication

Validation logic:
- File size must not exceed 40MB (41,943,040 bytes)
- File type must be PDF, MARKDOWN, or PLAIN_TEXT
- File name must not be empty
- User must exist and be active
- Check for duplicate content using hash

## Files / Modules Impacted

- backend/usecases/UploadDocument.java
- backend/usecases/models/UploadDocumentInput.java
- backend/usecases/models/UploadDocumentOutput.java
- backend/usecases/repositories/DocumentRepository.java

## Acceptance Criteria

Given a valid document upload request
When file size is under 40MB and type is supported
Then the document should be created with UPLOADED status

Given a document upload request with oversized file
When file size exceeds 40MB
Then the upload should be rejected with appropriate error message

Given a document upload request with unsupported file type
When file type is not PDF, Markdown, or plain text
Then the upload should be rejected with appropriate error message

## Testing Requirements

- Unit tests for UploadDocument use case
- Unit tests for input validation
- Unit tests for file size limits
- Unit tests for file type validation
- Unit tests for duplicate detection

## Dependencies / Preconditions

- Document and User domain entities must exist
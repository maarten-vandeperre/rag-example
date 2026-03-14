# Create Document domain entity

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create the core Document domain entity that represents an uploaded document in the system with its metadata and processing status.

## Scope

- Create Document domain entity with essential properties
- Include document status enumeration
- Add basic validation rules
- Implement value object for document metadata

## Out of Scope

- Persistence concerns
- File content processing
- REST API integration
- Frontend components

## Clean Architecture Placement

domain

## Execution Dependencies

- 0002-upload_documents_and_chat_with_private_knowledge_base-create_postgresql_container_setup.md

## Implementation Details

Create a Document entity with the following properties:
- documentId (unique identifier)
- fileName (original file name)
- fileSize (in bytes)
- fileType (PDF, MARKDOWN, PLAIN_TEXT)
- uploadedBy (user identifier)
- uploadedAt (timestamp)
- status (UPLOADED, PROCESSING, READY, FAILED)
- contentHash (for deduplication)

Create DocumentStatus enumeration with values:
- UPLOADED
- PROCESSING  
- READY
- FAILED

Validation rules:
- fileName must not be null or empty
- fileSize must be positive and <= 40MB (41,943,040 bytes)
- fileType must be one of supported types
- uploadedBy must not be null
- uploadedAt must not be null

## Files / Modules Impacted

- backend/domain/entities/Document.java
- backend/domain/valueobjects/DocumentStatus.java
- backend/domain/valueobjects/FileType.java

## Acceptance Criteria

Given a Document entity is created
When all required properties are provided with valid values
Then the entity should be successfully instantiated

Given a Document entity is created with invalid file size
When fileSize exceeds 40MB
Then validation should fail

Given a Document entity is created with null fileName
When fileName is null or empty
Then validation should fail

## Testing Requirements

- Unit tests for Document entity creation
- Unit tests for validation rules
- Unit tests for DocumentStatus enumeration
- Unit tests for FileType enumeration

## Dependencies / Preconditions

- Database schema must be defined
# Create Document Library REST controller

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create REST controller for document library functionality to retrieve user documents and admin progress information.

## Scope

- Create REST controller for GET /api/documents
- Create REST controller for GET /api/admin/documents/progress
- Implement role-based access control
- Map between REST DTOs and use case models

## Out of Scope

- Document content retrieval
- Document deletion endpoints
- Pagination implementation
- Real-time status updates

## Clean Architecture Placement

interface adapters

## Execution Dependencies

- 0007-upload_documents_and_chat_with_private_knowledge_base-create_get_user_documents_usecase.md
- 0008-upload_documents_and_chat_with_private_knowledge_base-create_get_admin_progress_usecase.md

## Implementation Details

Create DocumentLibraryController with:
- GET /api/documents endpoint for user documents
- GET /api/admin/documents/progress endpoint for admin progress
- Role-based access control validation
- Query parameter handling for admin view

Create DocumentListResponse DTO with:
- documents (list of DocumentSummaryDto)
- totalCount (int)

Create DocumentSummaryDto with:
- documentId (string)
- fileName (string)
- fileSize (long)
- fileType (string)
- status (string)
- uploadedBy (string)
- uploadedAt (timestamp)
- lastUpdated (timestamp)

Create AdminProgressResponse DTO with:
- statistics (ProcessingStatisticsDto)
- failedDocuments (list of FailedDocumentDto)
- processingDocuments (list of ProcessingDocumentDto)

Query parameters for GET /api/documents:
- includeAll (boolean, admin only): show all documents vs own documents

HTTP status codes:
- 200 OK: successful retrieval
- 403 Forbidden: insufficient permissions for admin endpoints
- 500 Internal Server Error: processing error

## Files / Modules Impacted

- backend/adapters/rest/DocumentLibraryController.java
- backend/adapters/rest/dto/DocumentListResponse.java
- backend/adapters/rest/dto/DocumentSummaryDto.java
- backend/adapters/rest/dto/AdminProgressResponse.java
- backend/adapters/rest/dto/ProcessingStatisticsDto.java
- backend/adapters/rest/dto/FailedDocumentDto.java
- backend/adapters/rest/dto/ProcessingDocumentDto.java

## Acceptance Criteria

Given a standard user requests their documents
When GET /api/documents is called
Then only the user's documents should be returned

Given an admin user requests all documents
When GET /api/documents?includeAll=true is called
Then all documents should be returned

Given an admin user requests progress information
When GET /api/admin/documents/progress is called
Then processing statistics and failed documents should be returned

Given a standard user tries to access admin progress
When GET /api/admin/documents/progress is called
Then HTTP 403 should be returned

## Testing Requirements

- Unit tests for DocumentLibraryController
- Integration tests for document retrieval endpoints
- Tests for role-based access control
- Tests for query parameter handling
- Tests for error response formats

## Dependencies / Preconditions

- GetUserDocuments and GetAdminProgress use cases must be implemented
- User authentication and role validation must be available
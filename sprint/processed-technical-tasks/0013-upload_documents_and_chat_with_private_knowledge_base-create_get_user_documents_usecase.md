# Create GetUserDocuments use case

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create the GetUserDocuments use case that retrieves documents for display in the document library with role-based access control.

## Scope

- Create GetUserDocuments use case with input/output models
- Implement role-based document filtering (standard users see only their documents, admins see all)
- Include document status and metadata for library display

## Out of Scope

- Document content retrieval
- File download functionality
- Document deletion
- Pagination implementation

## Clean Architecture Placement

usecases

## Execution Dependencies

- 0001-upload_documents_and_chat_with_private_knowledge_base-create_document_domain_entity.md
- 0002-upload_documents_and_chat_with_private_knowledge_base-create_user_domain_entity.md
- 0004-upload_documents_and_chat_with_private_knowledge_base-create_upload_document_usecase.md

## Implementation Details

Create GetUserDocumentsInput with:
- userId (string)
- includeAllDocuments (boolean, for admin users)

Create GetUserDocumentsOutput with:
- documents (list of DocumentSummary)
- totalCount (int)

Create DocumentSummary with:
- documentId (string)
- fileName (string)
- fileSize (long)
- fileType (FileType)
- status (DocumentStatus)
- uploadedBy (string, user identifier)
- uploadedAt (timestamp)
- lastUpdated (timestamp)

Business rules:
- Standard users can only see their own documents
- Admin users can see all documents when includeAllDocuments is true
- Admin users can see only their own documents when includeAllDocuments is false
- Documents should be ordered by uploadedAt descending (newest first)

Extend DocumentRepository interface with:
- findByUploadedBy(String userId) method
- findAll() method
- findByStatus(DocumentStatus status) method

## Files / Modules Impacted

- backend/usecases/GetUserDocuments.java
- backend/usecases/models/GetUserDocumentsInput.java
- backend/usecases/models/GetUserDocumentsOutput.java
- backend/usecases/models/DocumentSummary.java
- backend/usecases/repositories/DocumentRepository.java (extend existing)

## Acceptance Criteria

Given a standard user requests their documents
When the user has uploaded documents
Then only the user's documents should be returned

Given an admin user requests all documents
When includeAllDocuments is true
Then all documents from all users should be returned

Given an admin user requests only their documents
When includeAllDocuments is false
Then only the admin's documents should be returned

Given documents exist with different statuses
When documents are retrieved
Then all statuses should be included in the response

## Testing Requirements

- Unit tests for GetUserDocuments use case
- Unit tests for role-based access control
- Unit tests for document filtering
- Unit tests for sorting by upload date
- Mock tests for DocumentRepository methods

## Dependencies / Preconditions

- Document and User entities must exist
- DocumentRepository interface must exist
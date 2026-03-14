# Create GetAdminProgress use case

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create the GetAdminProgress use case that allows admin users to monitor document processing status across all users, with focus on failed imports.

## Scope

- Create GetAdminProgress use case with input/output models
- Implement admin-only access control
- Provide detailed processing statistics and failed document information

## Out of Scope

- Document retry mechanisms
- Processing queue management
- Real-time progress updates
- Document deletion from admin view

## Clean Architecture Placement

usecases

## Execution Dependencies

- 0001-upload_documents_and_chat_with_private_knowledge_base-create_document_domain_entity.md
- 0002-upload_documents_and_chat_with_private_knowledge_base-create_user_domain_entity.md
- 0004-upload_documents_and_chat_with_private_knowledge_base-create_upload_document_usecase.md

## Implementation Details

Create GetAdminProgressInput with:
- adminUserId (string)

Create GetAdminProgressOutput with:
- processingStatistics (ProcessingStatistics)
- failedDocuments (list of FailedDocumentInfo)
- processingDocuments (list of ProcessingDocumentInfo)

Create ProcessingStatistics with:
- totalDocuments (int)
- uploadedCount (int)
- processingCount (int)
- readyCount (int)
- failedCount (int)

Create FailedDocumentInfo with:
- documentId (string)
- fileName (string)
- uploadedBy (string)
- uploadedAt (timestamp)
- failureReason (string)
- fileSize (long)

Create ProcessingDocumentInfo with:
- documentId (string)
- fileName (string)
- uploadedBy (string)
- uploadedAt (timestamp)
- processingStartedAt (timestamp)

Business rules:
- Only admin users can access this functionality
- Failed documents should be ordered by failure time (newest first)
- Processing documents should show how long they've been processing

Extend DocumentRepository interface with:
- getProcessingStatistics() method
- findFailedDocuments() method
- findProcessingDocuments() method

## Files / Modules Impacted

- backend/usecases/GetAdminProgress.java
- backend/usecases/models/GetAdminProgressInput.java
- backend/usecases/models/GetAdminProgressOutput.java
- backend/usecases/models/ProcessingStatistics.java
- backend/usecases/models/FailedDocumentInfo.java
- backend/usecases/models/ProcessingDocumentInfo.java
- backend/usecases/repositories/DocumentRepository.java (extend existing)

## Acceptance Criteria

Given an admin user requests progress information
When the request is made
Then processing statistics should be returned

Given there are failed documents
When admin requests progress
Then failed documents should be listed with failure reasons

Given there are documents currently processing
When admin requests progress
Then processing documents should be shown with timing information

Given a non-admin user requests progress information
When the request is made
Then access should be denied

## Testing Requirements

- Unit tests for GetAdminProgress use case
- Unit tests for admin access control
- Unit tests for statistics calculation
- Unit tests for failed document information
- Mock tests for DocumentRepository methods

## Dependencies / Preconditions

- Document and User entities must exist
- DocumentRepository interface must exist
- User role validation must be available
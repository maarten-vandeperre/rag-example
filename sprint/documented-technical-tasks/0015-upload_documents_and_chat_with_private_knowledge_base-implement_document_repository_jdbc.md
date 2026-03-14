# Implement Document repository with JDBC

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Implement the DocumentRepository interface using JDBC for document persistence, including all required methods for document CRUD operations and queries.

## Scope

- Implement DocumentRepository interface using JDBC
- Create database schema for documents table
- Implement all repository methods with explicit SQL
- Add proper error handling and connection management

## Out of Scope

- ORM frameworks (JPA, Hibernate)
- Database migration tools
- Connection pooling configuration
- Database-specific optimizations

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0001-upload_documents_and_chat_with_private_knowledge_base-create_document_domain_entity.md
- 0004-upload_documents_and_chat_with_private_knowledge_base-create_upload_document_usecase.md
- 0007-upload_documents_and_chat_with_private_knowledge_base-create_get_user_documents_usecase.md
- 0008-upload_documents_and_chat_with_private_knowledge_base-create_get_admin_progress_usecase.md

## Implementation Details

Create documents table schema:
```sql
CREATE TABLE documents (
    document_id VARCHAR(255) PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    content_hash VARCHAR(255),
    last_updated TIMESTAMP NOT NULL,
    failure_reason TEXT,
    processing_started_at TIMESTAMP
);
```

Implement JdbcDocumentRepository with methods:
- save(Document document)
- findById(String documentId)
- findByUploadedBy(String userId)
- findAll()
- findByStatus(DocumentStatus status)
- findByContentHash(String hash)
- getProcessingStatistics()
- findFailedDocuments()
- findProcessingDocuments()
- updateStatus(String documentId, DocumentStatus status)

Use explicit SQL statements for all operations.
Handle SQLException appropriately.
Map database rows to Document entities.

## Files / Modules Impacted

- backend/infrastructure/persistence/JdbcDocumentRepository.java
- backend/infrastructure/persistence/DocumentRowMapper.java
- backend/infrastructure/database/schema.sql

## Acceptance Criteria

Given a Document entity is saved
When save() method is called
Then the document should be persisted to the database

Given a document exists in the database
When findById() is called with valid ID
Then the correct Document entity should be returned

Given multiple documents exist for a user
When findByUploadedBy() is called
Then only documents uploaded by that user should be returned

Given documents exist with different statuses
When getProcessingStatistics() is called
Then accurate counts for each status should be returned

## Testing Requirements

- Unit tests for JdbcDocumentRepository
- Integration tests with in-memory database
- Tests for SQL query correctness
- Tests for error handling
- Tests for row mapping

## Dependencies / Preconditions

- Document entity must exist
- DocumentRepository interface must be defined
- Database connection configuration must be available
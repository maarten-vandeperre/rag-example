# Integrate knowledge extraction into document upload pipeline

## Related User Story

Knowledge Graph Integration - Next Steps

## Objective

Integrate knowledge extraction and graph building into the existing document upload pipeline so that knowledge graphs are automatically created when documents are processed.

## Scope

- Wire knowledge extraction use cases into document processing
- Ensure knowledge processing runs in parallel with search indexing
- Implement graceful degradation if knowledge processing fails
- Add knowledge processing status to document processing results

## Out of Scope

- Modifying existing search functionality
- Creating new document upload endpoints
- Frontend changes for document upload

## Clean Architecture Placement

- usecases (document processing integration)
- infrastructure (pipeline orchestration)

## Execution Dependencies

- 0072-knowledge_graph_integration-replace_stub_user_management_facade_with_real_implementation.md

## Implementation Details

### Document Processing Pipeline Integration
- Modify document processing use cases to include knowledge extraction
- Add ExtractKnowledgeFromDocument use case to the pipeline
- Add BuildKnowledgeGraph use case to the pipeline
- Ensure knowledge processing runs asynchronously alongside search processing

### Parallel Processing Implementation
- Use CompletableFuture or similar for parallel execution
- Ensure search processing is not blocked by knowledge processing
- Implement proper error isolation between search and knowledge processing

### Error Handling and Graceful Degradation
- Knowledge processing failures should not prevent document search functionality
- Log knowledge processing errors appropriately
- Return partial success status when search succeeds but knowledge fails

### Status Reporting
- Add knowledge processing status to ProcessDocumentOutput
- Include knowledge graph ID in successful processing results
- Provide meaningful error messages for knowledge processing failures

### Configuration
- Add configuration options for enabling/disabling knowledge processing
- Add timeout configuration for knowledge extraction
- Add retry configuration for transient failures

## Files / Modules Impacted

- `backend/document-management/src/main/java/com/rag/app/document/usecases/ProcessDocument.java`
- `backend/document-management/src/main/java/com/rag/app/document/usecases/models/ProcessDocumentOutput.java`
- Document processing configuration classes
- Integration tests for document processing

## Acceptance Criteria

**Given** a document is uploaded for processing
**When** the document processing pipeline runs
**Then** both search indexing and knowledge extraction should be attempted

**Given** search processing succeeds but knowledge processing fails
**When** the document processing completes
**Then** the document should be searchable and the failure should be logged

**Given** knowledge processing succeeds
**When** the document processing completes
**Then** the knowledge graph should be created and accessible via the knowledge graph API

**Given** knowledge processing is disabled in configuration
**When** a document is processed
**Then** only search processing should run

## Testing Requirements

- Integration tests for parallel processing
- Tests for error scenarios (knowledge processing failure)
- Tests for graceful degradation
- Performance tests to ensure search processing is not impacted
- Tests for configuration options

## Dependencies / Preconditions

- ExtractKnowledgeFromDocument use case is implemented
- BuildKnowledgeGraph use case is implemented
- Knowledge graph repository is functional
- Document processing pipeline exists and is working
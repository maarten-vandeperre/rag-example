# Implement knowledge graph extraction during document processing

## Related User Story

Knowledge Graph Creation Fix

## Objective

Implement automatic knowledge graph creation during document upload and processing. Ensure that when documents are uploaded and processed, knowledge graphs are automatically extracted and stored in Neo4j for later browsing and analysis.

## Scope

- Integrate knowledge extraction into the document processing pipeline
- Ensure knowledge graphs are created automatically during document upload
- Implement entity and relationship extraction from document content
- Store extracted knowledge in Neo4j database
- Verify knowledge graphs are accessible via the knowledge graph API

## Out of Scope

- Advanced NLP model training
- Complex ontology management
- Real-time knowledge extraction
- Knowledge graph visualization UI

## Clean Architecture Placement

- usecases (knowledge extraction integration)
- domain (knowledge extraction services)
- infrastructure (Neo4j integration)

## Execution Dependencies

- 0085-chat_functionality_fix-investigate_and_fix_unable_to_process_chat_query_error.md

## Implementation Details

### Integrate Knowledge Extraction into Document Processing
- Modify the document processing pipeline to include knowledge extraction
- Ensure knowledge extraction runs in parallel with vector indexing
- Implement graceful degradation if knowledge processing fails
- Add knowledge processing status to document processing results

### Implement Knowledge Extraction Service
- Create or configure the knowledge extraction service implementation
- Implement entity recognition (persons, organizations, concepts)
- Implement relationship extraction between entities
- Add confidence scoring for extracted knowledge
- Support multiple document types (PDF, Markdown, text)

### Configure Neo4j Integration
- Ensure Neo4j connection is properly configured
- Verify knowledge graph repository is working
- Test knowledge graph storage and retrieval
- Implement proper error handling for Neo4j operations

### Update Document Processing Flow
- Modify ProcessDocument use case to include knowledge extraction
- Add knowledge graph creation step after text extraction
- Ensure knowledge processing doesn't block document completion
- Add proper logging and error handling for knowledge extraction

### Verify Knowledge Graph Creation
- Test that uploaded documents generate knowledge graphs
- Verify entities and relationships are properly extracted
- Check that knowledge graphs are stored in Neo4j
- Test knowledge graph API endpoints return created graphs

## Files / Modules Impacted

- `backend/src/main/java/com/rag/app/usecases/ProcessDocument.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/`
- Knowledge extraction service implementations
- Neo4j repository and configuration
- Document processing configuration

## Acceptance Criteria

**Given** a document is uploaded for processing
**When** the document processing pipeline runs
**Then** a knowledge graph should be automatically created and stored

**Given** a document contains entities and relationships
**When** knowledge extraction is performed
**Then** relevant entities and relationships should be identified and stored

**Given** knowledge graphs are created during processing
**When** accessing the knowledge graph API
**Then** the created graphs should be visible and accessible

**Given** knowledge processing fails for a document
**When** the document processing completes
**Then** the document should still be marked as READY for search, with knowledge processing marked as failed

## Testing Requirements

- Test document upload with knowledge graph creation
- Verify entities and relationships are extracted correctly
- Test Neo4j storage and retrieval of knowledge graphs
- Test knowledge graph API endpoints with created data
- Verify graceful degradation when knowledge processing fails
- Test with different document types and content

## Dependencies / Preconditions

- Neo4j service is running and accessible
- Knowledge extraction service is implemented
- Document processing pipeline is functional
- Knowledge graph API endpoints are working
- Application startup issues are resolved
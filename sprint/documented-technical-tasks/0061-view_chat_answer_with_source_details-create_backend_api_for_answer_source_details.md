# Create Backend API for Answer Source Details

## Related User Story

User Story: view_chat_answer_with_source_details

## Objective

Create backend REST API endpoints to retrieve detailed source information for chat answers, including source snippets, metadata, and full document access capabilities.

## Scope

- Create REST endpoint for retrieving answer source details
- Implement source snippet extraction and formatting
- Add endpoint for full document retrieval
- Handle source availability and error cases
- Implement proper authentication and authorization
- Add caching for performance optimization

## Out of Scope

- Frontend integration (separate task)
- UI components (separate task)
- Chat answer generation (existing functionality)
- Document upload and processing (existing functionality)

## Clean Architecture Placement

interface adapters

## Execution Dependencies

None

## Implementation Details

### API Endpoints Design

**Primary Endpoint: Get Answer Source Details**
```
GET /api/chat/answers/{answerId}/sources
```

**Response Format:**
```json
{
  "answerId": "uuid",
  "sources": [
    {
      "sourceId": "uuid",
      "documentId": "uuid",
      "fileName": "document.pdf",
      "fileType": "PDF",
      "snippet": {
        "content": "Relevant text snippet from the document...",
        "startPosition": 1250,
        "endPosition": 1450,
        "context": "Additional context around the snippet..."
      },
      "metadata": {
        "title": "Document Title",
        "author": "Document Author",
        "createdAt": "2024-01-15T10:30:00Z",
        "pageNumber": 5,
        "chunkIndex": 12
      },
      "relevanceScore": 0.85,
      "available": true
    }
  ],
  "totalSources": 3,
  "availableSources": 2
}
```

**Secondary Endpoint: Get Full Document**
```
GET /api/documents/{documentId}/content
```

**Response Format:**
```json
{
  "documentId": "uuid",
  "fileName": "document.pdf",
  "fileType": "PDF",
  "content": "Full document content...",
  "metadata": {
    "title": "Document Title",
    "author": "Document Author",
    "createdAt": "2024-01-15T10:30:00Z",
    "fileSize": 1024000,
    "pageCount": 25
  },
  "available": true
}
```

### Backend Implementation

**REST Controller:**
```java
@Path("/api/chat/answers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnswerSourceController {

    @GET
    @Path("/{answerId}/sources")
    public Response getAnswerSources(
        @PathParam("answerId") String answerId,
        @Context SecurityContext securityContext
    ) {
        // Implementation
    }
}

@Path("/api/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentContentController {

    @GET
    @Path("/{documentId}/content")
    public Response getDocumentContent(
        @PathParam("documentId") String documentId,
        @Context SecurityContext securityContext
    ) {
        // Implementation
    }
}
```

**Use Case Implementation:**
```java
@ApplicationScoped
public class GetAnswerSourceDetailsUseCase {
    
    public AnswerSourceDetailsResponse getSourceDetails(
        String answerId, 
        String userId
    ) {
        // 1. Validate answer exists and user has access
        // 2. Retrieve source references for the answer
        // 3. Extract snippets from source documents
        // 4. Format response with metadata
        // 5. Handle unavailable sources gracefully
    }
}
```

### Data Model Extensions

**Answer Source Reference (Domain Model):**
```java
public class AnswerSourceReference {
    private final String sourceId;
    private final String documentId;
    private final String answerId;
    private final int startPosition;
    private final int endPosition;
    private final double relevanceScore;
    private final int chunkIndex;
    
    // Constructor, getters, business methods
}
```

**Source Snippet (Value Object):**
```java
public class SourceSnippet {
    private final String content;
    private final int startPosition;
    private final int endPosition;
    private final String context;
    
    // Constructor, getters, validation
}
```

### Repository Layer

**Answer Source Repository:**
```java
public interface AnswerSourceRepository {
    List<AnswerSourceReference> findSourcesByAnswerId(String answerId);
    Optional<DocumentChunk> findChunkBySourceReference(AnswerSourceReference source);
    boolean isSourceAvailable(String documentId);
}
```

**JDBC Implementation:**
```sql
-- Query to get source references for an answer
SELECT 
    asr.source_id,
    asr.document_id,
    asr.start_position,
    asr.end_position,
    asr.relevance_score,
    asr.chunk_index,
    d.file_name,
    d.file_type,
    d.title,
    d.created_at
FROM answer_source_references asr
JOIN documents d ON asr.document_id = d.id
WHERE asr.answer_id = ?
ORDER BY asr.relevance_score DESC;
```

### Error Handling

**Error Scenarios:**
1. Answer not found or user doesn't have access
2. Source documents are no longer available
3. Document chunks cannot be retrieved
4. Snippet extraction fails

**Error Response Format:**
```json
{
  "error": {
    "code": "SOURCE_NOT_AVAILABLE",
    "message": "One or more source documents are no longer available",
    "details": {
      "unavailableSources": ["doc-id-1", "doc-id-2"]
    }
  }
}
```

### Performance Optimization

**Caching Strategy:**
- Cache source details for frequently accessed answers
- Cache document content with appropriate TTL
- Use Redis for distributed caching

**Database Optimization:**
- Index on answer_id for source reference queries
- Optimize chunk retrieval queries
- Consider read replicas for heavy read operations

## Files / Modules Impacted

- `backend/src/main/java/com/rag/app/api/AnswerSourceController.java` - New REST controller
- `backend/src/main/java/com/rag/app/api/DocumentContentController.java` - New REST controller
- `backend/src/main/java/com/rag/app/usecases/GetAnswerSourceDetailsUseCase.java` - New use case
- `backend/src/main/java/com/rag/app/usecases/GetDocumentContentUseCase.java` - New use case
- `backend/src/main/java/com/rag/app/domain/AnswerSourceReference.java` - New domain model
- `backend/src/main/java/com/rag/app/domain/SourceSnippet.java` - New value object
- `backend/src/main/java/com/rag/app/infrastructure/AnswerSourceRepository.java` - New repository interface
- `backend/src/main/java/com/rag/app/infrastructure/JdbcAnswerSourceRepository.java` - JDBC implementation
- `backend/src/main/resources/db/migration/` - Database schema updates
- `backend/src/test/java/com/rag/app/api/AnswerSourceControllerTest.java` - Controller tests
- `backend/src/test/java/com/rag/app/usecases/GetAnswerSourceDetailsUseCaseTest.java` - Use case tests

## Acceptance Criteria

**Given** a user requests source details for a valid answer ID
**When** the API endpoint is called with proper authentication
**Then** the response should include all available source snippets with metadata

**Given** an answer has multiple sources
**When** source details are requested
**Then** sources should be returned ordered by relevance score

**Given** some sources for an answer are no longer available
**When** source details are requested
**Then** available sources should be returned with appropriate warnings for unavailable ones

**Given** a user requests full document content
**When** they have access to the document
**Then** the complete document content should be returned with metadata

**Given** a user requests source details for a non-existent answer
**When** the API endpoint is called
**Then** a 404 error should be returned with appropriate error message

**Given** a user requests source details without proper authentication
**When** the API endpoint is called
**Then** a 401 error should be returned

## Testing Requirements

- Unit tests for use cases and domain logic
- Integration tests for REST endpoints
- Repository tests with test database
- Performance tests for caching behavior
- Security tests for authentication and authorization
- Error handling tests for various failure scenarios
- Load tests for concurrent access patterns

## Dependencies / Preconditions

- Existing chat answer storage system
- Document storage and retrieval system
- User authentication and authorization system
- Database schema for storing answer-source relationships
- Caching infrastructure (Redis)

## Implementation Notes

### Database Schema Requirements

**Answer Source References Table:**
```sql
CREATE TABLE answer_source_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    answer_id UUID NOT NULL REFERENCES chat_answers(id),
    document_id UUID NOT NULL REFERENCES documents(id),
    source_id UUID NOT NULL,
    start_position INTEGER NOT NULL,
    end_position INTEGER NOT NULL,
    relevance_score DECIMAL(3,2) NOT NULL,
    chunk_index INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_answer_sources (answer_id),
    INDEX idx_document_sources (document_id)
);
```

### Security Considerations

- Validate user access to both answers and documents
- Implement rate limiting for API endpoints
- Sanitize document content before returning
- Log access to sensitive documents for audit

### Performance Considerations

- Implement pagination for large numbers of sources
- Use streaming for large document content
- Consider CDN for frequently accessed documents
- Optimize database queries with proper indexing

### Error Recovery

- Implement retry logic for transient failures
- Graceful degradation when sources are unavailable
- Clear error messages for different failure scenarios
- Monitoring and alerting for system health
# Fix "No source snippet is available" Error - Implement Chunk Retrieval

## Related User Story

User Story: view_chat_answer_with_source_details

## Objective

Fix the "No source snippet is available" error by implementing proper chunk retrieval and snippet extraction functionality, ensuring users can see the actual document chunks that were used to generate chat answers.

## Scope

- Investigate and fix the source snippet retrieval mechanism
- Implement proper chunk-to-snippet mapping
- Add chunk content extraction from vector database
- Ensure snippet context and positioning are correctly calculated
- Add fallback mechanisms for missing chunk data
- Implement debugging capabilities to trace chunk relationships
- Add proper error handling for chunk retrieval failures

## Out of Scope

- Vector database schema changes (unless absolutely necessary)
- Chat answer generation logic (existing functionality)
- Frontend snippet display (separate task)
- Performance optimization (focus on functionality first)

## Clean Architecture Placement

interface adapters / infrastructure

## Execution Dependencies

- 0061-view_chat_answer_with_source_details-create_backend_api_for_answer_source_details.md

## Implementation Details

### Root Cause Analysis

**Current Issue:**
The error "No source snippet is available" indicates that the system cannot retrieve or extract the actual text chunks that were used during the RAG (Retrieval-Augmented Generation) process to generate chat answers.

**Potential Causes:**
1. Missing relationship between chat answers and source chunks
2. Chunk data not being stored during answer generation
3. Incorrect chunk retrieval from vector database (Weaviate)
4. Missing snippet extraction logic
5. Broken chunk-to-document mapping

### Enhanced Answer-Source Relationship Storage

**During Answer Generation (Fix):**
```java
@ApplicationScoped
public class ChatAnswerService {
    
    public ChatAnswer generateAnswer(String query, String userId) {
        // 1. Retrieve relevant chunks from vector database
        List<DocumentChunk> relevantChunks = vectorSearchService.searchSimilarChunks(query, userId);
        
        // 2. Generate answer using LLM
        String answerContent = llmService.generateAnswer(query, relevantChunks);
        
        // 3. Create answer entity
        ChatAnswer answer = new ChatAnswer(answerContent, userId);
        chatAnswerRepository.save(answer);
        
        // 4. CRITICAL: Store chunk relationships with snippet details
        for (int i = 0; i < relevantChunks.size(); i++) {
            DocumentChunk chunk = relevantChunks.get(i);
            
            AnswerSourceReference sourceRef = new AnswerSourceReference(
                answer.getId(),
                chunk.getDocumentId(),
                chunk.getId(), // chunk ID as source ID
                chunk.getStartPosition(),
                chunk.getEndPosition(),
                chunk.getRelevanceScore(),
                i // chunk index in the result set
            );
            
            answerSourceRepository.save(sourceRef);
        }
        
        return answer;
    }
}
```

### Enhanced Chunk Retrieval Implementation

**Weaviate Chunk Retrieval Service:**
```java
@ApplicationScoped
public class WeaviateChunkRetrievalService {
    
    @Inject
    WeaviateClient weaviateClient;
    
    public Optional<DocumentChunk> retrieveChunkById(String chunkId) {
        try {
            // Query Weaviate for specific chunk
            Result<GraphQLResponse> result = weaviateClient.graphQL()
                .get()
                .withClassName("DocumentChunk")
                .withFields("documentId chunkIndex textContent fileName fileType createdAt chunkSize")
                .withWhere(WhereFilter.builder()
                    .path(new String[]{"_id"})
                    .operator(Operator.Equal)
                    .valueText(chunkId)
                    .build())
                .run();
            
            if (result.hasErrors()) {
                logger.error("Error retrieving chunk {}: {}", chunkId, result.getError());
                return Optional.empty();
            }
            
            GraphQLResponse response = result.getResult();
            List<Map<String, Object>> chunks = extractChunksFromResponse(response);
            
            if (chunks.isEmpty()) {
                logger.warn("Chunk not found in Weaviate: {}", chunkId);
                return Optional.empty();
            }
            
            return Optional.of(mapToDocumentChunk(chunks.get(0), chunkId));
            
        } catch (Exception e) {
            logger.error("Failed to retrieve chunk from Weaviate: {}", chunkId, e);
            return Optional.empty();
        }
    }
    
    public List<DocumentChunk> retrieveChunksByIds(List<String> chunkIds) {
        if (chunkIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Batch query for multiple chunks
            WhereFilter whereFilter = WhereFilter.builder()
                .path(new String[]{"_id"})
                .operator(Operator.ContainsAny)
                .valueTextArray(chunkIds.toArray(new String[0]))
                .build();
            
            Result<GraphQLResponse> result = weaviateClient.graphQL()
                .get()
                .withClassName("DocumentChunk")
                .withFields("documentId chunkIndex textContent fileName fileType createdAt chunkSize")
                .withWhere(whereFilter)
                .run();
            
            if (result.hasErrors()) {
                logger.error("Error retrieving chunks: {}", result.getError());
                return Collections.emptyList();
            }
            
            GraphQLResponse response = result.getResult();
            List<Map<String, Object>> chunks = extractChunksFromResponse(response);
            
            return chunks.stream()
                .map(chunk -> mapToDocumentChunk(chunk, extractChunkId(chunk)))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Failed to retrieve chunks from Weaviate", e);
            return Collections.emptyList();
        }
    }
    
    private DocumentChunk mapToDocumentChunk(Map<String, Object> chunkData, String chunkId) {
        return new DocumentChunk(
            chunkId,
            (String) chunkData.get("documentId"),
            (Integer) chunkData.get("chunkIndex"),
            (String) chunkData.get("textContent"),
            (String) chunkData.get("fileName"),
            (String) chunkData.get("fileType"),
            parseTimestamp((String) chunkData.get("createdAt")),
            (Integer) chunkData.get("chunkSize")
        );
    }
}
```

### Enhanced Answer Source Repository

**Updated Repository Implementation:**
```java
@ApplicationScoped
public class JdbcAnswerSourceRepository implements AnswerSourceRepository {
    
    @Override
    public List<AnswerSourceDetails> getSourceDetailsForAnswer(String answerId) {
        String sql = """
            SELECT 
                asr.source_id as chunk_id,
                asr.document_id,
                asr.start_position,
                asr.end_position,
                asr.relevance_score,
                asr.chunk_index,
                d.file_name,
                d.file_type,
                d.title,
                d.created_at as document_created_at
            FROM answer_source_references asr
            LEFT JOIN documents d ON asr.document_id = d.id
            WHERE asr.answer_id = ?
            ORDER BY asr.relevance_score DESC, asr.chunk_index ASC
            """;
        
        List<AnswerSourceDetails> sourceDetails = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, answerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String chunkId = rs.getString("chunk_id");
                    
                    // Retrieve actual chunk content from Weaviate
                    Optional<DocumentChunk> chunk = chunkRetrievalService.retrieveChunkById(chunkId);
                    
                    if (chunk.isPresent()) {
                        DocumentChunk chunkData = chunk.get();
                        
                        SourceSnippet snippet = createSnippetFromChunk(chunkData);
                        
                        AnswerSourceDetails details = new AnswerSourceDetails(
                            chunkId,
                            rs.getString("document_id"),
                            rs.getString("file_name"),
                            rs.getString("file_type"),
                            snippet,
                            createMetadata(rs, chunkData),
                            rs.getDouble("relevance_score"),
                            true // available
                        );
                        
                        sourceDetails.add(details);
                    } else {
                        // Create unavailable source entry
                        AnswerSourceDetails unavailable = createUnavailableSource(rs);
                        sourceDetails.add(unavailable);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve source details for answer: {}", answerId, e);
            throw new RepositoryException("Failed to retrieve source details", e);
        }
        
        return sourceDetails;
    }
    
    private SourceSnippet createSnippetFromChunk(DocumentChunk chunk) {
        String content = chunk.getTextContent();
        
        // For chunks, the entire chunk content is the snippet
        // Add some context if the chunk is part of a larger document
        String context = extractContext(chunk);
        
        return new SourceSnippet(
            content,
            0, // start position within chunk
            content.length(), // end position within chunk
            context
        );
    }
    
    private String extractContext(DocumentChunk chunk) {
        // Try to get surrounding chunks for context
        try {
            List<DocumentChunk> surroundingChunks = getSurroundingChunks(
                chunk.getDocumentId(), 
                chunk.getChunkIndex()
            );
            
            if (surroundingChunks.size() > 1) {
                // Create context from surrounding chunks
                return surroundingChunks.stream()
                    .filter(c -> !c.getId().equals(chunk.getId()))
                    .map(DocumentChunk::getTextContent)
                    .collect(Collectors.joining(" ... "));
            }
        } catch (Exception e) {
            logger.debug("Could not retrieve context for chunk: {}", chunk.getId(), e);
        }
        
        return null; // No additional context available
    }
}
```

### Enhanced Use Case Implementation

**GetAnswerSourceDetailsUseCase with Debugging:**
```java
@ApplicationScoped
public class GetAnswerSourceDetailsUseCase {
    
    @Inject
    AnswerSourceRepository answerSourceRepository;
    
    @Inject
    WeaviateChunkRetrievalService chunkRetrievalService;
    
    public AnswerSourceDetailsResponse getSourceDetails(String answerId, String userId) {
        logger.debug("Retrieving source details for answer: {}", answerId);
        
        // 1. Validate answer exists and user has access
        validateAnswerAccess(answerId, userId);
        
        // 2. Get source references from database
        List<AnswerSourceDetails> sourceDetails = answerSourceRepository.getSourceDetailsForAnswer(answerId);
        
        logger.debug("Found {} source references for answer {}", sourceDetails.size(), answerId);
        
        if (sourceDetails.isEmpty()) {
            logger.warn("No source references found for answer: {}", answerId);
            return createEmptyResponse(answerId);
        }
        
        // 3. Separate available and unavailable sources
        List<AnswerSourceDetails> availableSources = sourceDetails.stream()
            .filter(AnswerSourceDetails::isAvailable)
            .collect(Collectors.toList());
            
        List<AnswerSourceDetails> unavailableSources = sourceDetails.stream()
            .filter(source -> !source.isAvailable())
            .collect(Collectors.toList());
        
        logger.debug("Available sources: {}, Unavailable sources: {}", 
            availableSources.size(), unavailableSources.size());
        
        // 4. Log debugging information
        logSourceDebuggingInfo(answerId, sourceDetails);
        
        return new AnswerSourceDetailsResponse(
            answerId,
            sourceDetails,
            sourceDetails.size(),
            availableSources.size()
        );
    }
    
    private void logSourceDebuggingInfo(String answerId, List<AnswerSourceDetails> sources) {
        logger.debug("=== Source Details Debug Info for Answer {} ===", answerId);
        
        for (int i = 0; i < sources.size(); i++) {
            AnswerSourceDetails source = sources.get(i);
            logger.debug("Source {}: ID={}, Document={}, Available={}, SnippetLength={}", 
                i + 1, 
                source.getSourceId(), 
                source.getDocumentId(),
                source.isAvailable(),
                source.getSnippet() != null ? source.getSnippet().getContent().length() : 0
            );
            
            if (source.getSnippet() != null) {
                String preview = source.getSnippet().getContent().substring(
                    0, Math.min(100, source.getSnippet().getContent().length())
                );
                logger.debug("Snippet preview: {}...", preview);
            }
        }
        
        logger.debug("=== End Debug Info ===");
    }
}
```

### Database Schema Verification

**Ensure Answer Source References Table Exists:**
```sql
-- Migration script to create/update answer_source_references table
CREATE TABLE IF NOT EXISTS answer_source_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    answer_id UUID NOT NULL,
    document_id UUID NOT NULL,
    source_id VARCHAR(255) NOT NULL, -- This should be the chunk ID from Weaviate
    start_position INTEGER,
    end_position INTEGER,
    relevance_score DECIMAL(5,4),
    chunk_index INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_answer_source_answer 
        FOREIGN KEY (answer_id) REFERENCES chat_answers(id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_source_document 
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_answer_source_references_answer_id 
    ON answer_source_references(answer_id);
CREATE INDEX IF NOT EXISTS idx_answer_source_references_document_id 
    ON answer_source_references(document_id);
CREATE INDEX IF NOT EXISTS idx_answer_source_references_source_id 
    ON answer_source_references(source_id);
```

### Enhanced REST Controller with Debugging

**Updated AnswerSourceController:**
```java
@Path("/api/chat/answers")
@Produces(MediaType.APPLICATION_JSON)
public class AnswerSourceController {
    
    @Inject
    GetAnswerSourceDetailsUseCase getAnswerSourceDetailsUseCase;
    
    @GET
    @Path("/{answerId}/sources")
    public Response getAnswerSources(
        @PathParam("answerId") String answerId,
        @QueryParam("debug") @DefaultValue("false") boolean debug,
        @Context SecurityContext securityContext
    ) {
        try {
            String userId = extractUserId(securityContext);
            
            logger.info("Getting sources for answer: {} (user: {}, debug: {})", 
                answerId, userId, debug);
            
            AnswerSourceDetailsResponse response = getAnswerSourceDetailsUseCase
                .getSourceDetails(answerId, userId);
            
            if (debug) {
                // Add debug information to response
                response.setDebugInfo(createDebugInfo(answerId, response));
            }
            
            return Response.ok(response).build();
            
        } catch (AnswerNotFoundException e) {
            return Response.status(404)
                .entity(Map.of("error", "Answer not found"))
                .build();
        } catch (Exception e) {
            logger.error("Failed to get sources for answer: {}", answerId, e);
            return Response.status(500)
                .entity(Map.of("error", "Failed to retrieve source details"))
                .build();
        }
    }
    
    private Map<String, Object> createDebugInfo(String answerId, AnswerSourceDetailsResponse response) {
        return Map.of(
            "answerId", answerId,
            "totalSourcesFound", response.getTotalSources(),
            "availableSourcesCount", response.getAvailableSources(),
            "sourceIds", response.getSources().stream()
                .map(AnswerSourceDetails::getSourceId)
                .collect(Collectors.toList()),
            "chunkRetrievalStatus", response.getSources().stream()
                .collect(Collectors.toMap(
                    AnswerSourceDetails::getSourceId,
                    AnswerSourceDetails::isAvailable
                ))
        );
    }
}
```

## Files / Modules Impacted

- `backend/src/main/java/com/rag/app/services/ChatAnswerService.java` - Fix answer generation to store chunk relationships
- `backend/src/main/java/com/rag/app/services/WeaviateChunkRetrievalService.java` - New service for chunk retrieval
- `backend/src/main/java/com/rag/app/infrastructure/JdbcAnswerSourceRepository.java` - Enhanced chunk-to-snippet mapping
- `backend/src/main/java/com/rag/app/usecases/GetAnswerSourceDetailsUseCase.java` - Enhanced with debugging
- `backend/src/main/java/com/rag/app/api/AnswerSourceController.java` - Add debug endpoint
- `backend/src/main/java/com/rag/app/domain/AnswerSourceReference.java` - Enhanced domain model
- `backend/src/main/java/com/rag/app/domain/SourceSnippet.java` - Enhanced snippet creation
- `backend/src/main/resources/db/migration/V003__fix_answer_source_references.sql` - Database schema fix
- `backend/src/test/java/com/rag/app/services/WeaviateChunkRetrievalServiceTest.java` - New tests

## Acceptance Criteria

**Given** a chat answer was generated using document chunks
**When** a user requests source details for that answer
**Then** the actual text chunks used should be returned as source snippets

**Given** chunk data exists in Weaviate for an answer's sources
**When** the source details API is called
**Then** the chunk content should be successfully retrieved and formatted as snippets

**Given** some chunks are no longer available in Weaviate
**When** source details are requested
**Then** available chunks should be returned with clear indication of unavailable ones

**Given** the debug mode is enabled
**When** source details are requested
**Then** additional debugging information should be included in the response

**Given** an answer has no stored source references
**When** source details are requested
**Then** a clear message should indicate no sources are available rather than showing an error

**Given** chunk retrieval fails for technical reasons
**When** this occurs
**Then** appropriate error logging should occur and fallback behavior should be triggered

## Testing Requirements

- Unit tests for WeaviateChunkRetrievalService
- Integration tests for chunk-to-snippet conversion
- Tests for answer generation with proper source reference storage
- Database migration tests for schema updates
- Error handling tests for missing chunks
- Performance tests for batch chunk retrieval
- End-to-end tests for the complete source details flow

## Dependencies / Preconditions

- Weaviate vector database must be accessible
- Answer generation process must be storing chunk relationships
- Database schema must support answer-source references
- Existing chat answers may need to be regenerated to have proper source references

## Implementation Notes

### Debugging Strategy
- Add comprehensive logging at each step of chunk retrieval
- Include debug endpoint to trace source reference chain
- Log Weaviate query performance and results
- Track chunk availability statistics

### Data Migration Considerations
- Existing answers without source references will show "No sources available"
- Consider regenerating recent answers to populate source references
- Implement gradual migration strategy for historical data

### Performance Optimization
- Batch chunk retrieval from Weaviate when possible
- Cache frequently accessed chunks
- Optimize database queries for source references
- Consider async chunk retrieval for better response times

### Error Recovery
- Graceful handling of missing chunks in Weaviate
- Fallback to document-level sources when chunks unavailable
- Clear error messages for different failure scenarios
- Retry mechanisms for transient Weaviate failures

### Monitoring and Alerting
- Track chunk retrieval success rates
- Monitor Weaviate query performance
- Alert on high rates of missing chunks
- Log source snippet generation statistics
# Persist Answer-to-Chunk Relationships for Source Detail Retrieval

## Related User Story

User Story: view_chat_answer_with_source_details

## Objective

Persist the relationship between generated chat answers and the exact retrieved document chunks used to produce them, so source detail requests can always return the real snippet content and no longer depend on in-memory state.

## Problem

Currently:
- Chat answers may be returned even when history persistence fails
- Answer source detail retrieval can return:
  - "answer not found"
  - "No source snippet is available"
- Answer-to-chunk mapping is not reliably persisted
- Source detail resolution breaks after backend restart or when in-memory chunk state is unavailable

## Scope

- Persist answer records reliably
- Persist answer-to-source-chunk relationships in the database
- Persist enough chunk/snippet metadata to reconstruct source details
- Update source detail retrieval to use persisted answer-source records
- Ensure chunk snippet text is available even after restart
- Add fallback behavior for missing/deleted documents or chunks
- Improve logging and error handling around answer persistence and source retrieval

## Out of Scope

- Frontend redesign
- LLM prompt changes
- Vector DB schema redesign unless absolutely required

## Clean Architecture Placement

interface adapters / infrastructure

## Execution Dependencies

- 0061-view_chat_answer_with_source_details-create_backend_api_for_answer_source_details.md

## Implementation Details

### Database Schema Design

**Answer Source References Table:**
```sql
CREATE TABLE answer_source_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    answer_id UUID NOT NULL,
    document_id UUID NOT NULL,
    chunk_id VARCHAR(255) NOT NULL, -- Weaviate chunk ID or stable identifier
    snippet_content TEXT NOT NULL, -- The actual text snippet used
    snippet_context TEXT, -- Additional context around the snippet
    start_position INTEGER, -- Position within the document/chunk
    end_position INTEGER, -- End position within the document/chunk
    relevance_score DECIMAL(5,4) NOT NULL, -- Relevance score from vector search
    source_order INTEGER NOT NULL, -- Order of this source in the result set
    document_title VARCHAR(500), -- Cached document title
    document_filename VARCHAR(255), -- Cached document filename
    document_file_type VARCHAR(50), -- Cached document type
    page_number INTEGER, -- Page number if applicable (PDF)
    chunk_index INTEGER, -- Index of chunk within document
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_answer_source_answer 
        FOREIGN KEY (answer_id) REFERENCES chat_answers(id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_source_document 
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX idx_answer_source_references_answer_id ON answer_source_references(answer_id);
CREATE INDEX idx_answer_source_references_document_id ON answer_source_references(document_id);
CREATE INDEX idx_answer_source_references_chunk_id ON answer_source_references(chunk_id);
CREATE INDEX idx_answer_source_references_source_order ON answer_source_references(answer_id, source_order);
```

### Enhanced Domain Models

**AnswerSourceReference Domain Model:**
```java
public class AnswerSourceReference {
    private final String id;
    private final String answerId;
    private final String documentId;
    private final String chunkId;
    private final String snippetContent;
    private final String snippetContext;
    private final Integer startPosition;
    private final Integer endPosition;
    private final Double relevanceScore;
    private final Integer sourceOrder;
    private final String documentTitle;
    private final String documentFilename;
    private final String documentFileType;
    private final Integer pageNumber;
    private final Integer chunkIndex;
    private final Instant createdAt;

    public AnswerSourceReference(
        String answerId,
        String documentId,
        String chunkId,
        String snippetContent,
        Double relevanceScore,
        Integer sourceOrder
    ) {
        this.id = UUID.randomUUID().toString();
        this.answerId = requireNonNull(answerId, "Answer ID cannot be null");
        this.documentId = requireNonNull(documentId, "Document ID cannot be null");
        this.chunkId = requireNonNull(chunkId, "Chunk ID cannot be null");
        this.snippetContent = requireNonNull(snippetContent, "Snippet content cannot be null");
        this.relevanceScore = requireNonNull(relevanceScore, "Relevance score cannot be null");
        this.sourceOrder = requireNonNull(sourceOrder, "Source order cannot be null");
        this.createdAt = Instant.now();
        // Other fields can be null initially
    }

    // Builder pattern for optional fields
    public static class Builder {
        private final AnswerSourceReference reference;
        
        public Builder(String answerId, String documentId, String chunkId, 
                      String snippetContent, Double relevanceScore, Integer sourceOrder) {
            this.reference = new AnswerSourceReference(answerId, documentId, chunkId, 
                                                     snippetContent, relevanceScore, sourceOrder);
        }
        
        public Builder withContext(String context) {
            return new Builder(reference).snippetContext(context);
        }
        
        public Builder withPositions(Integer start, Integer end) {
            return new Builder(reference).startPosition(start).endPosition(end);
        }
        
        public Builder withDocumentMetadata(String title, String filename, String fileType) {
            return new Builder(reference)
                .documentTitle(title)
                .documentFilename(filename)
                .documentFileType(fileType);
        }
        
        public Builder withPageInfo(Integer pageNumber, Integer chunkIndex) {
            return new Builder(reference).pageNumber(pageNumber).chunkIndex(chunkIndex);
        }
        
        public AnswerSourceReference build() {
            return reference;
        }
    }
    
    // Getters and business methods
}
```

### Transactional Answer Persistence

**Enhanced ChatAnswerService:**
```java
@ApplicationScoped
@Transactional
public class ChatAnswerService {
    
    @Inject
    ChatAnswerRepository chatAnswerRepository;
    
    @Inject
    AnswerSourceReferenceRepository answerSourceRepository;
    
    @Inject
    VectorSearchService vectorSearchService;
    
    @Inject
    LLMService llmService;
    
    @Inject
    DocumentRepository documentRepository;
    
    public ChatAnswerResponse generateAnswer(String query, String userId) {
        try {
            // 1. Retrieve relevant chunks from vector database
            List<RetrievedChunk> retrievedChunks = vectorSearchService.searchSimilarChunks(query, userId);
            
            if (retrievedChunks.isEmpty()) {
                return createAnswerWithoutSources(query, userId);
            }
            
            // 2. Generate answer using LLM
            String answerContent = llmService.generateAnswer(query, retrievedChunks);
            
            // 3. Create and persist answer
            ChatAnswer answer = new ChatAnswer(answerContent, query, userId);
            ChatAnswer persistedAnswer = chatAnswerRepository.save(answer);
            
            // 4. CRITICAL: Persist source references in same transaction
            List<AnswerSourceReference> sourceReferences = createSourceReferences(
                persistedAnswer.getId(), 
                retrievedChunks
            );
            
            for (AnswerSourceReference sourceRef : sourceReferences) {
                answerSourceRepository.save(sourceRef);
            }
            
            logger.info("Successfully persisted answer {} with {} source references", 
                persistedAnswer.getId(), sourceReferences.size());
            
            return new ChatAnswerResponse(persistedAnswer, sourceReferences.size());
            
        } catch (Exception e) {
            logger.error("Failed to generate and persist answer for query: {}", query, e);
            // Transaction will be rolled back automatically
            throw new ChatAnswerGenerationException("Failed to generate answer", e);
        }
    }
    
    private List<AnswerSourceReference> createSourceReferences(
        String answerId, 
        List<RetrievedChunk> retrievedChunks
    ) {
        List<AnswerSourceReference> references = new ArrayList<>();
        
        for (int i = 0; i < retrievedChunks.size(); i++) {
            RetrievedChunk chunk = retrievedChunks.get(i);
            
            try {
                // Get document metadata for caching
                Optional<Document> document = documentRepository.findById(chunk.getDocumentId());
                
                AnswerSourceReference.Builder builder = new AnswerSourceReference.Builder(
                    answerId,
                    chunk.getDocumentId(),
                    chunk.getChunkId(),
                    chunk.getContent(), // This is the actual snippet content
                    chunk.getRelevanceScore(),
                    i // source order
                );
                
                // Add document metadata if available
                if (document.isPresent()) {
                    Document doc = document.get();
                    builder.withDocumentMetadata(
                        doc.getTitle(),
                        doc.getFileName(),
                        doc.getFileType().toString()
                    );
                }
                
                // Add chunk-specific metadata
                builder.withPageInfo(chunk.getPageNumber(), chunk.getChunkIndex());
                
                // Add position information if available
                if (chunk.getStartPosition() != null && chunk.getEndPosition() != null) {
                    builder.withPositions(chunk.getStartPosition(), chunk.getEndPosition());
                }
                
                // Add context if available
                if (chunk.getContext() != null) {
                    builder.withContext(chunk.getContext());
                }
                
                references.add(builder.build());
                
            } catch (Exception e) {
                logger.warn("Failed to create source reference for chunk {}: {}", 
                    chunk.getChunkId(), e.getMessage());
                // Continue with other chunks - don't fail the entire operation
            }
        }
        
        return references;
    }
    
    private ChatAnswerResponse createAnswerWithoutSources(String query, String userId) {
        // Handle case where no sources are found
        String answerContent = llmService.generateGeneralAnswer(query);
        ChatAnswer answer = new ChatAnswer(answerContent, query, userId);
        ChatAnswer persistedAnswer = chatAnswerRepository.save(answer);
        
        logger.info("Generated answer {} without sources for query: {}", 
            persistedAnswer.getId(), query);
        
        return new ChatAnswerResponse(persistedAnswer, 0);
    }
}
```

### Enhanced Source Detail Retrieval

**Updated GetAnswerSourceDetailsUseCase:**
```java
@ApplicationScoped
public class GetAnswerSourceDetailsUseCase {
    
    @Inject
    ChatAnswerRepository chatAnswerRepository;
    
    @Inject
    AnswerSourceReferenceRepository answerSourceRepository;
    
    @Inject
    DocumentRepository documentRepository;
    
    public AnswerSourceDetailsResponse getSourceDetails(String answerId, String userId) {
        logger.debug("Retrieving source details for answer: {} (user: {})", answerId, userId);
        
        // 1. Validate answer exists and user has access
        Optional<ChatAnswer> answer = chatAnswerRepository.findById(answerId);
        if (answer.isEmpty()) {
            throw new AnswerNotFoundException("Answer not found: " + answerId);
        }
        
        if (!answer.get().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not have access to this answer");
        }
        
        // 2. Retrieve persisted source references
        List<AnswerSourceReference> sourceReferences = answerSourceRepository
            .findByAnswerIdOrderBySourceOrder(answerId);
        
        logger.debug("Found {} persisted source references for answer {}", 
            sourceReferences.size(), answerId);
        
        if (sourceReferences.isEmpty()) {
            logger.info("No source references found for answer: {}", answerId);
            return createEmptySourceResponse(answerId);
        }
        
        // 3. Convert to response format with availability checking
        List<SourceDetail> sourceDetails = sourceReferences.stream()
            .map(this::convertToSourceDetail)
            .collect(Collectors.toList());
        
        // 4. Count available vs unavailable sources
        long availableCount = sourceDetails.stream()
            .mapToLong(detail -> detail.isAvailable() ? 1 : 0)
            .sum();
        
        logger.debug("Source details for answer {}: {} total, {} available", 
            answerId, sourceDetails.size(), availableCount);
        
        return new AnswerSourceDetailsResponse(
            answerId,
            sourceDetails,
            sourceDetails.size(),
            (int) availableCount
        );
    }
    
    private SourceDetail convertToSourceDetail(AnswerSourceReference reference) {
        try {
            // Check if document still exists
            boolean documentExists = reference.getDocumentId() != null && 
                documentRepository.existsById(reference.getDocumentId());
            
            if (!documentExists) {
                logger.debug("Document {} no longer exists for source reference {}", 
                    reference.getDocumentId(), reference.getId());
                return createUnavailableSourceDetail(reference, "Document no longer available");
            }
            
            // Create source snippet from persisted data
            SourceSnippet snippet = new SourceSnippet(
                reference.getSnippetContent(),
                reference.getStartPosition() != null ? reference.getStartPosition() : 0,
                reference.getEndPosition() != null ? reference.getEndPosition() : reference.getSnippetContent().length(),
                reference.getSnippetContext()
            );
            
            // Create source metadata
            SourceMetadata metadata = new SourceMetadata(
                reference.getDocumentTitle(),
                reference.getDocumentFilename(),
                reference.getDocumentFileType(),
                reference.getPageNumber(),
                reference.getChunkIndex(),
                reference.getCreatedAt()
            );
            
            return new SourceDetail(
                reference.getChunkId(),
                reference.getDocumentId(),
                reference.getDocumentFilename() != null ? reference.getDocumentFilename() : "Unknown",
                reference.getDocumentFileType() != null ? reference.getDocumentFileType() : "UNKNOWN",
                snippet,
                metadata,
                reference.getRelevanceScore(),
                true // available
            );
            
        } catch (Exception e) {
            logger.warn("Failed to convert source reference {} to source detail: {}", 
                reference.getId(), e.getMessage());
            return createUnavailableSourceDetail(reference, "Error loading source details");
        }
    }
    
    private SourceDetail createUnavailableSourceDetail(AnswerSourceReference reference, String reason) {
        return new SourceDetail(
            reference.getChunkId(),
            reference.getDocumentId(),
            reference.getDocumentFilename() != null ? reference.getDocumentFilename() : "Unknown",
            reference.getDocumentFileType() != null ? reference.getDocumentFileType() : "UNKNOWN",
            null, // no snippet available
            null, // no metadata available
            reference.getRelevanceScore(),
            false, // not available
            reason
        );
    }
    
    private AnswerSourceDetailsResponse createEmptySourceResponse(String answerId) {
        return new AnswerSourceDetailsResponse(
            answerId,
            Collections.emptyList(),
            0,
            0
        );
    }
}
```

### Repository Implementation

**AnswerSourceReferenceRepository:**
```java
public interface AnswerSourceReferenceRepository {
    AnswerSourceReference save(AnswerSourceReference reference);
    List<AnswerSourceReference> findByAnswerIdOrderBySourceOrder(String answerId);
    Optional<AnswerSourceReference> findById(String id);
    void deleteByAnswerId(String answerId);
    boolean existsByAnswerId(String answerId);
    long countByAnswerId(String answerId);
}

@ApplicationScoped
public class JdbcAnswerSourceReferenceRepository implements AnswerSourceReferenceRepository {
    
    @Inject
    DataSource dataSource;
    
    @Override
    public AnswerSourceReference save(AnswerSourceReference reference) {
        String sql = """
            INSERT INTO answer_source_references (
                id, answer_id, document_id, chunk_id, snippet_content, snippet_context,
                start_position, end_position, relevance_score, source_order,
                document_title, document_filename, document_file_type,
                page_number, chunk_index, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, reference.getId());
            stmt.setString(2, reference.getAnswerId());
            stmt.setString(3, reference.getDocumentId());
            stmt.setString(4, reference.getChunkId());
            stmt.setString(5, reference.getSnippetContent());
            stmt.setString(6, reference.getSnippetContext());
            setIntegerOrNull(stmt, 7, reference.getStartPosition());
            setIntegerOrNull(stmt, 8, reference.getEndPosition());
            stmt.setDouble(9, reference.getRelevanceScore());
            stmt.setInt(10, reference.getSourceOrder());
            stmt.setString(11, reference.getDocumentTitle());
            stmt.setString(12, reference.getDocumentFilename());
            stmt.setString(13, reference.getDocumentFileType());
            setIntegerOrNull(stmt, 14, reference.getPageNumber());
            setIntegerOrNull(stmt, 15, reference.getChunkIndex());
            stmt.setTimestamp(16, Timestamp.from(reference.getCreatedAt()));
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected != 1) {
                throw new RepositoryException("Failed to save answer source reference");
            }
            
            logger.debug("Saved answer source reference: {}", reference.getId());
            return reference;
            
        } catch (SQLException e) {
            logger.error("Failed to save answer source reference: {}", reference.getId(), e);
            throw new RepositoryException("Database error saving answer source reference", e);
        }
    }
    
    @Override
    public List<AnswerSourceReference> findByAnswerIdOrderBySourceOrder(String answerId) {
        String sql = """
            SELECT id, answer_id, document_id, chunk_id, snippet_content, snippet_context,
                   start_position, end_position, relevance_score, source_order,
                   document_title, document_filename, document_file_type,
                   page_number, chunk_index, created_at
            FROM answer_source_references
            WHERE answer_id = ?
            ORDER BY source_order ASC
            """;
        
        List<AnswerSourceReference> references = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, answerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    references.add(mapResultSetToReference(rs));
                }
            }
            
            logger.debug("Found {} source references for answer: {}", references.size(), answerId);
            return references;
            
        } catch (SQLException e) {
            logger.error("Failed to find source references for answer: {}", answerId, e);
            throw new RepositoryException("Database error finding source references", e);
        }
    }
    
    private AnswerSourceReference mapResultSetToReference(ResultSet rs) throws SQLException {
        return new AnswerSourceReference(
            rs.getString("id"),
            rs.getString("answer_id"),
            rs.getString("document_id"),
            rs.getString("chunk_id"),
            rs.getString("snippet_content"),
            rs.getString("snippet_context"),
            getIntegerOrNull(rs, "start_position"),
            getIntegerOrNull(rs, "end_position"),
            rs.getDouble("relevance_score"),
            rs.getInt("source_order"),
            rs.getString("document_title"),
            rs.getString("document_filename"),
            rs.getString("document_file_type"),
            getIntegerOrNull(rs, "page_number"),
            getIntegerOrNull(rs, "chunk_index"),
            rs.getTimestamp("created_at").toInstant()
        );
    }
    
    private void setIntegerOrNull(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value != null) {
            stmt.setInt(index, value);
        } else {
            stmt.setNull(index, Types.INTEGER);
        }
    }
    
    private Integer getIntegerOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
```

### Enhanced Error Handling

**Custom Exceptions:**
```java
public class ChatAnswerGenerationException extends RuntimeException {
    public ChatAnswerGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class AnswerPersistenceException extends RuntimeException {
    public AnswerPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class SourceReferencePersistenceException extends RuntimeException {
    public SourceReferencePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Database Migration

**Migration Script:**
```sql
-- V004__create_answer_source_references_table.sql

CREATE TABLE answer_source_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    answer_id UUID NOT NULL,
    document_id UUID,
    chunk_id VARCHAR(255) NOT NULL,
    snippet_content TEXT NOT NULL,
    snippet_context TEXT,
    start_position INTEGER,
    end_position INTEGER,
    relevance_score DECIMAL(5,4) NOT NULL,
    source_order INTEGER NOT NULL,
    document_title VARCHAR(500),
    document_filename VARCHAR(255),
    document_file_type VARCHAR(50),
    page_number INTEGER,
    chunk_index INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_answer_source_answer 
        FOREIGN KEY (answer_id) REFERENCES chat_answers(id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_source_document 
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL,
    CONSTRAINT chk_relevance_score 
        CHECK (relevance_score >= 0.0 AND relevance_score <= 1.0),
    CONSTRAINT chk_source_order 
        CHECK (source_order >= 0),
    CONSTRAINT chk_positions 
        CHECK (start_position IS NULL OR end_position IS NULL OR start_position <= end_position)
);

-- Indexes for performance
CREATE INDEX idx_answer_source_references_answer_id ON answer_source_references(answer_id);
CREATE INDEX idx_answer_source_references_document_id ON answer_source_references(document_id);
CREATE INDEX idx_answer_source_references_chunk_id ON answer_source_references(chunk_id);
CREATE INDEX idx_answer_source_references_source_order ON answer_source_references(answer_id, source_order);
CREATE INDEX idx_answer_source_references_created_at ON answer_source_references(created_at);

-- Add comment for documentation
COMMENT ON TABLE answer_source_references IS 'Stores the relationship between chat answers and the document chunks used to generate them';
COMMENT ON COLUMN answer_source_references.snippet_content IS 'The actual text content from the chunk that was used';
COMMENT ON COLUMN answer_source_references.relevance_score IS 'Vector similarity score between 0.0 and 1.0';
COMMENT ON COLUMN answer_source_references.source_order IS 'Order of this source in the original retrieval results';
```

## Files / Modules Impacted

- `backend/src/main/java/com/rag/app/services/ChatAnswerService.java` - Enhanced with transactional persistence
- `backend/src/main/java/com/rag/app/domain/AnswerSourceReference.java` - New domain model
- `backend/src/main/java/com/rag/app/infrastructure/AnswerSourceReferenceRepository.java` - New repository interface
- `backend/src/main/java/com/rag/app/infrastructure/JdbcAnswerSourceReferenceRepository.java` - JDBC implementation
- `backend/src/main/java/com/rag/app/usecases/GetAnswerSourceDetailsUseCase.java` - Updated to use persisted data
- `backend/src/main/java/com/rag/app/api/ChatController.java` - Enhanced error handling
- `backend/src/main/java/com/rag/app/api/AnswerSourceController.java` - Updated for persistent data
- `backend/src/main/resources/db/migration/V004__create_answer_source_references_table.sql` - Database migration
- `backend/src/test/java/com/rag/app/services/ChatAnswerServiceTest.java` - Enhanced tests
- `backend/src/test/java/com/rag/app/infrastructure/AnswerSourceReferenceRepositoryTest.java` - New repository tests
- `backend/src/test/java/integration/ChatAnswerSourceIntegrationTest.java` - New integration tests

## Expected Behavior

**Given** a chat answer is generated from retrieved chunks
**When** the answer is returned to the frontend
**Then** the backend must persist:
- The answer ID
- The source document IDs
- The chunk IDs or stable chunk identifiers
- The snippet text used for the source
- Source ordering/ranking
- Relevance score
- Any start/end/context metadata needed for display

**Given** a user opens source details later
**When** `GET /api/chat/answers/{answerId}/sources` is called
**Then** the API should return:
- The persisted source entries
- Actual snippet text
- Available/unavailable flags
- Stable metadata for each source

**Given** the backend has restarted
**When** source details are requested for a previously generated answer
**Then** the source details should still work without relying on in-memory chunk storage

## Acceptance Criteria

- ✅ New chat answers persist source references in durable storage
- ✅ Source details API works after backend restart
- ✅ Source details API returns real snippet text for persisted answers
- ✅ "Answer not found" no longer occurs for successfully returned new answers
- ✅ "No source snippet is available" no longer occurs for successfully generated answers with retrieved sources
- ✅ Missing/deleted source data is reported clearly and gracefully
- ✅ Logs clearly indicate persistence failures and source-retrieval failures
- ✅ Compile and tests pass

## Testing Requirements

- Unit tests for answer-source persistence
- Unit tests for source retrieval from persisted data
- Integration test:
  1. Upload document
  2. Ask question
  3. Capture answer ID
  4. Request source details
  5. Verify snippet content returned
- Integration test covering backend restart simulation or fresh repository state
- Failure test for missing chunk/document data
- Transaction rollback tests for persistence failures
- Performance tests for large numbers of source references

## Dependencies / Preconditions

- Existing chat answer generation system
- Database with transaction support
- Vector search service providing chunk data
- Document repository for metadata lookup

## Implementation Notes

### Transaction Management
- Use `@Transactional` to ensure answer and source references are persisted atomically
- If source reference persistence fails, the entire answer generation should fail
- Provide clear error messages when persistence fails

### Performance Considerations
- Batch insert source references when possible
- Index on answer_id for fast source retrieval
- Consider pagination for answers with many sources
- Cache document metadata to reduce database queries

### Data Integrity
- Foreign key constraints ensure referential integrity
- Cascade delete when answers are deleted
- Set null when documents are deleted (graceful degradation)
- Validate relevance scores and positions

### Error Recovery
- Clear logging for all persistence operations
- Graceful handling of missing documents
- Fallback behavior when chunk data is incomplete
- Monitoring for persistence failure rates

### Migration Strategy
- New table creation with proper constraints
- Existing answers without source references will show "No sources available"
- Consider backfilling recent answers if chunk data is still available
- Gradual rollout with feature flags if needed
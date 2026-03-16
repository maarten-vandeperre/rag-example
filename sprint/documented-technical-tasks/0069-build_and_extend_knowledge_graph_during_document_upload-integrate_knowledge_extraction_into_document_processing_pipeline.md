# Integrate Knowledge Extraction into Document Processing Pipeline

## Related User Story

User Story: build_and_extend_knowledge_graph_during_document_upload

## Objective

Extend the existing document processing pipeline to include knowledge extraction and graph building alongside the current search-related processing, ensuring both processes run in parallel and provide comprehensive status tracking.

## Scope

- Update ProcessDocument use case to include knowledge extraction
- Create parallel processing for search and knowledge graph workflows
- Add knowledge processing status tracking to document entities
- Implement error handling and warning collection for knowledge processing
- Ensure knowledge processing failures don't block search processing
- Add configuration options for enabling/disabling knowledge extraction

## Out of Scope

- Changes to the document upload UI (handled in separate tasks)
- Neo4j implementation details (handled in infrastructure tasks)
- Knowledge extraction algorithms (handled in infrastructure tasks)
- Graph administration interface (handled in separate tasks)

## Clean Architecture Placement

usecases / domain

## Execution Dependencies

- 0067-build_and_extend_knowledge_graph_during_document_upload-create_knowledge_graph_domain_entities.md
- 0068-build_and_extend_knowledge_graph_during_document_upload-create_knowledge_extraction_use_cases.md

## Implementation Details

### Enhanced Document Entity

**Updated Document Entity with Knowledge Processing Status:**
```java
public final class Document {
    // Existing fields...
    private final KnowledgeProcessingStatus knowledgeProcessingStatus;
    private final List<String> knowledgeProcessingWarnings;
    private final String knowledgeProcessingError;
    private final Instant knowledgeProcessingStartedAt;
    private final Instant knowledgeProcessingCompletedAt;
    private final GraphId associatedGraphId;
    
    // Enhanced constructor and builder methods
    
    public Document withKnowledgeProcessingStarted() {
        return new Document.Builder(this)
            .knowledgeProcessingStatus(KnowledgeProcessingStatus.IN_PROGRESS)
            .knowledgeProcessingStartedAt(Instant.now())
            .build();
    }
    
    public Document withKnowledgeProcessingCompleted(
        GraphId graphId, 
        List<String> warnings
    ) {
        return new Document.Builder(this)
            .knowledgeProcessingStatus(KnowledgeProcessingStatus.COMPLETED)
            .knowledgeProcessingCompletedAt(Instant.now())
            .associatedGraphId(graphId)
            .knowledgeProcessingWarnings(warnings)
            .build();
    }
    
    public Document withKnowledgeProcessingFailed(String error) {
        return new Document.Builder(this)
            .knowledgeProcessingStatus(KnowledgeProcessingStatus.FAILED)
            .knowledgeProcessingCompletedAt(Instant.now())
            .knowledgeProcessingError(error)
            .build();
    }
    
    public boolean isKnowledgeProcessingComplete() {
        return knowledgeProcessingStatus == KnowledgeProcessingStatus.COMPLETED ||
               knowledgeProcessingStatus == KnowledgeProcessingStatus.FAILED ||
               knowledgeProcessingStatus == KnowledgeProcessingStatus.SKIPPED;
    }
    
    public boolean hasKnowledgeProcessingWarnings() {
        return knowledgeProcessingWarnings != null && !knowledgeProcessingWarnings.isEmpty();
    }
}
```

**KnowledgeProcessingStatus Enum:**
```java
public enum KnowledgeProcessingStatus {
    NOT_STARTED("Knowledge processing not started"),
    IN_PROGRESS("Knowledge processing in progress"),
    COMPLETED("Knowledge processing completed successfully"),
    COMPLETED_WITH_WARNINGS("Knowledge processing completed with warnings"),
    FAILED("Knowledge processing failed"),
    SKIPPED("Knowledge processing skipped due to insufficient content"),
    DISABLED("Knowledge processing disabled for this document type");
    
    private final String description;
    
    KnowledgeProcessingStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == COMPLETED_WITH_WARNINGS || 
               this == FAILED || this == SKIPPED || this == DISABLED;
    }
    
    public boolean isSuccessful() {
        return this == COMPLETED || this == COMPLETED_WITH_WARNINGS;
    }
}
```

### Enhanced ProcessDocument Use Case

**Updated ProcessDocument Use Case:**
```java
@ApplicationScoped
public final class ProcessDocument {
    
    private final DocumentRepository documentRepository;
    private final ContentExtractor contentExtractor;
    private final VectorStore vectorStore;
    private final ExtractKnowledgeFromDocument extractKnowledgeFromDocument;
    private final BuildKnowledgeGraph buildKnowledgeGraph;
    private final KnowledgeProcessingConfiguration knowledgeConfig;
    private final Clock clock;
    
    public ProcessDocument(
        DocumentRepository documentRepository,
        ContentExtractor contentExtractor,
        VectorStore vectorStore,
        ExtractKnowledgeFromDocument extractKnowledgeFromDocument,
        BuildKnowledgeGraph buildKnowledgeGraph,
        KnowledgeProcessingConfiguration knowledgeConfig,
        Clock clock
    ) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository cannot be null");
        this.contentExtractor = Objects.requireNonNull(contentExtractor, "contentExtractor cannot be null");
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore cannot be null");
        this.extractKnowledgeFromDocument = Objects.requireNonNull(extractKnowledgeFromDocument, "extractKnowledgeFromDocument cannot be null");
        this.buildKnowledgeGraph = Objects.requireNonNull(buildKnowledgeGraph, "buildKnowledgeGraph cannot be null");
        this.knowledgeConfig = Objects.requireNonNull(knowledgeConfig, "knowledgeConfig cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }
    
    public ProcessDocumentOutput execute(ProcessDocumentInput input) {
        Objects.requireNonNull(input, "input cannot be null");
        
        Document document = documentRepository.findById(input.documentId())
            .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + input.documentId()));
        
        if (document.getStatus() != DocumentStatus.UPLOADED) {
            throw new IllegalStateException("Document must be in UPLOADED status to be processed");
        }
        
        // Start processing
        document = document.withStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);
        
        try {
            // Extract content for both search and knowledge processing
            String extractedContent = contentExtractor.extractContent(document);
            
            // Process in parallel: search processing and knowledge processing
            CompletableFuture<SearchProcessingResult> searchFuture = processForSearch(document, extractedContent);
            CompletableFuture<KnowledgeProcessingResult> knowledgeFuture = processForKnowledge(document, extractedContent);
            
            // Wait for both to complete
            SearchProcessingResult searchResult = searchFuture.get();
            KnowledgeProcessingResult knowledgeResult = knowledgeFuture.get();
            
            // Update document with final status
            document = updateDocumentWithResults(document, searchResult, knowledgeResult);
            Document finalDocument = documentRepository.save(document);
            
            return createProcessingOutput(finalDocument, searchResult, knowledgeResult);
            
        } catch (Exception e) {
            // Handle processing failure
            document = document.withStatus(DocumentStatus.FAILED)
                              .withFailureReason("Processing failed: " + e.getMessage());
            documentRepository.save(document);
            
            throw new DocumentProcessingException("Failed to process document: " + input.documentId(), e);
        }
    }
    
    private CompletableFuture<SearchProcessingResult> processForSearch(Document document, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Existing search processing logic
                vectorStore.storeDocumentVectors(document.getDocumentId().toString(), content);
                return SearchProcessingResult.success();
            } catch (Exception e) {
                return SearchProcessingResult.failure("Search processing failed: " + e.getMessage());
            }
        });
    }
    
    private CompletableFuture<KnowledgeProcessingResult> processForKnowledge(Document document, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if knowledge processing is enabled
                if (!knowledgeConfig.isEnabledForDocumentType(document.getFileType())) {
                    return KnowledgeProcessingResult.skipped("Knowledge processing disabled for document type: " + document.getFileType());
                }
                
                // Update document status to show knowledge processing started
                Document updatedDoc = document.withKnowledgeProcessingStarted();
                documentRepository.save(updatedDoc);
                
                // Extract knowledge
                ExtractKnowledgeInput extractInput = new ExtractKnowledgeInput(
                    document.getDocumentId().toString(),
                    content,
                    document.getFileName(),
                    document.getFileType().toString(),
                    knowledgeConfig.getExtractionOptionsFor(document.getFileType())
                );
                
                ExtractKnowledgeOutput extractOutput = extractKnowledgeFromDocument.execute(extractInput);
                
                if (!extractOutput.isSuccessful()) {
                    return KnowledgeProcessingResult.failure(
                        "Knowledge extraction failed",
                        extractOutput.errors(),
                        extractOutput.warnings()
                    );
                }
                
                // Build knowledge graph if extraction was successful
                if (!extractOutput.extractedKnowledge().isEmpty()) {
                    BuildKnowledgeGraphInput buildInput = new BuildKnowledgeGraphInput(
                        knowledgeConfig.getDefaultGraphName(),
                        extractOutput.extractedKnowledge(),
                        true // allow merging
                    );
                    
                    BuildKnowledgeGraphOutput buildOutput = buildKnowledgeGraph.execute(buildInput);
                    
                    if (!buildOutput.success()) {
                        return KnowledgeProcessingResult.failure(
                            "Knowledge graph building failed: " + buildOutput.errorMessage(),
                            List.of(buildOutput.errorMessage()),
                            extractOutput.warnings()
                        );
                    }
                    
                    return KnowledgeProcessingResult.success(
                        buildOutput.graphId(),
                        extractOutput.warnings()
                    );
                } else {
                    return KnowledgeProcessingResult.skipped(
                        "No knowledge extracted from document",
                        extractOutput.warnings()
                    );
                }
                
            } catch (Exception e) {
                return KnowledgeProcessingResult.failure(
                    "Knowledge processing failed: " + e.getMessage(),
                    List.of(e.getMessage()),
                    List.of()
                );
            }
        });
    }
    
    private Document updateDocumentWithResults(
        Document document, 
        SearchProcessingResult searchResult, 
        KnowledgeProcessingResult knowledgeResult
    ) {
        // Update search processing status
        DocumentStatus finalStatus;
        String failureReason = null;
        
        if (!searchResult.isSuccessful()) {
            finalStatus = DocumentStatus.FAILED;
            failureReason = searchResult.getErrorMessage();
        } else {
            finalStatus = DocumentStatus.READY;
        }
        
        // Update knowledge processing status
        KnowledgeProcessingStatus knowledgeStatus;
        List<String> knowledgeWarnings = knowledgeResult.getWarnings();
        String knowledgeError = null;
        GraphId associatedGraphId = null;
        
        if (knowledgeResult.isSuccessful()) {
            knowledgeStatus = knowledgeWarnings.isEmpty() ? 
                KnowledgeProcessingStatus.COMPLETED : 
                KnowledgeProcessingStatus.COMPLETED_WITH_WARNINGS;
            associatedGraphId = knowledgeResult.getGraphId();
        } else if (knowledgeResult.isSkipped()) {
            knowledgeStatus = KnowledgeProcessingStatus.SKIPPED;
        } else {
            knowledgeStatus = KnowledgeProcessingStatus.FAILED;
            knowledgeError = knowledgeResult.getErrorMessage();
        }
        
        return document.withStatus(finalStatus)
                      .withFailureReason(failureReason)
                      .withKnowledgeProcessingStatus(knowledgeStatus)
                      .withKnowledgeProcessingWarnings(knowledgeWarnings)
                      .withKnowledgeProcessingError(knowledgeError)
                      .withAssociatedGraphId(associatedGraphId)
                      .withKnowledgeProcessingCompletedAt(clock.instant());
    }
}
```

### Processing Result Models

**SearchProcessingResult:**
```java
public record SearchProcessingResult(
    boolean successful,
    String errorMessage
) {
    public static SearchProcessingResult success() {
        return new SearchProcessingResult(true, null);
    }
    
    public static SearchProcessingResult failure(String errorMessage) {
        return new SearchProcessingResult(false, errorMessage);
    }
    
    public boolean isSuccessful() {
        return successful;
    }
}
```

**KnowledgeProcessingResult:**
```java
public record KnowledgeProcessingResult(
    boolean successful,
    boolean skipped,
    GraphId graphId,
    String errorMessage,
    List<String> errors,
    List<String> warnings
) {
    public static KnowledgeProcessingResult success(GraphId graphId, List<String> warnings) {
        return new KnowledgeProcessingResult(true, false, graphId, null, List.of(), warnings);
    }
    
    public static KnowledgeProcessingResult failure(String errorMessage, List<String> errors, List<String> warnings) {
        return new KnowledgeProcessingResult(false, false, null, errorMessage, errors, warnings);
    }
    
    public static KnowledgeProcessingResult skipped(String reason) {
        return new KnowledgeProcessingResult(false, true, null, reason, List.of(), List.of());
    }
    
    public static KnowledgeProcessingResult skipped(String reason, List<String> warnings) {
        return new KnowledgeProcessingResult(false, true, null, reason, List.of(), warnings);
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public boolean isSkipped() {
        return skipped;
    }
}
```

### Configuration

**KnowledgeProcessingConfiguration:**
```java
@ApplicationScoped
public final class KnowledgeProcessingConfiguration {
    
    private final Map<FileType, Boolean> enabledByFileType;
    private final Map<FileType, Map<String, Object>> extractionOptionsByFileType;
    private final String defaultGraphName;
    
    public KnowledgeProcessingConfiguration() {
        this.enabledByFileType = initializeEnabledByFileType();
        this.extractionOptionsByFileType = initializeExtractionOptions();
        this.defaultGraphName = "main-knowledge-graph";
    }
    
    public boolean isEnabledForDocumentType(FileType fileType) {
        return enabledByFileType.getOrDefault(fileType, false);
    }
    
    public Map<String, Object> getExtractionOptionsFor(FileType fileType) {
        return extractionOptionsByFileType.getOrDefault(fileType, getDefaultExtractionOptions());
    }
    
    public String getDefaultGraphName() {
        return defaultGraphName;
    }
    
    private Map<FileType, Boolean> initializeEnabledByFileType() {
        Map<FileType, Boolean> enabled = new HashMap<>();
        enabled.put(FileType.PDF, true);
        enabled.put(FileType.DOCX, true);
        enabled.put(FileType.TXT, true);
        enabled.put(FileType.MD, true);
        // Images and other types disabled by default
        enabled.put(FileType.JPG, false);
        enabled.put(FileType.PNG, false);
        return enabled;
    }
    
    private Map<FileType, Map<String, Object>> initializeExtractionOptions() {
        Map<FileType, Map<String, Object>> options = new HashMap<>();
        
        // PDF-specific options
        Map<String, Object> pdfOptions = new HashMap<>();
        pdfOptions.put("extract_entities", true);
        pdfOptions.put("extract_relationships", true);
        pdfOptions.put("min_confidence", 0.7);
        pdfOptions.put("max_entities_per_page", 50);
        options.put(FileType.PDF, pdfOptions);
        
        // Text-specific options
        Map<String, Object> textOptions = new HashMap<>();
        textOptions.put("extract_entities", true);
        textOptions.put("extract_relationships", true);
        textOptions.put("min_confidence", 0.6);
        textOptions.put("chunk_size", 1000);
        options.put(FileType.TXT, textOptions);
        
        return options;
    }
    
    private Map<String, Object> getDefaultExtractionOptions() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("extract_entities", true);
        defaults.put("extract_relationships", false);
        defaults.put("min_confidence", 0.5);
        return defaults;
    }
}
```

## Files / Modules Impacted

- `backend/document-management/src/main/java/com/rag/app/document/domain/entities/Document.java` - Enhanced with knowledge processing fields
- `backend/document-management/src/main/java/com/rag/app/document/domain/valueobjects/KnowledgeProcessingStatus.java` - New enum
- `backend/document-management/src/main/java/com/rag/app/document/usecases/ProcessDocument.java` - Enhanced with parallel processing
- `backend/document-management/src/main/java/com/rag/app/document/usecases/models/SearchProcessingResult.java` - New model
- `backend/document-management/src/main/java/com/rag/app/document/usecases/models/KnowledgeProcessingResult.java` - New model
- `backend/shared-kernel/src/main/java/com/rag/app/shared/configuration/KnowledgeProcessingConfiguration.java` - New configuration
- `backend/src/main/resources/schema.sql` - Updated with knowledge processing columns

## Expected Behavior

**Given** a document is uploaded and processing begins  
**When** both search and knowledge processing are enabled  
**Then** both processes should run in parallel and complete independently

**Given** search processing succeeds but knowledge processing fails  
**When** the document processing completes  
**Then** the document should be marked as READY with knowledge processing warnings

**Given** a document type has knowledge processing disabled  
**When** the document is processed  
**Then** knowledge processing should be skipped with appropriate status

## Acceptance Criteria

- ✅ Document processing supports parallel search and knowledge workflows
- ✅ Knowledge processing failures don't prevent search processing success
- ✅ Document entity tracks knowledge processing status and warnings
- ✅ Configuration allows enabling/disabling knowledge processing by file type
- ✅ Processing results provide detailed status for both workflows
- ✅ Error handling preserves existing search functionality
- ✅ Database schema supports knowledge processing metadata

## Testing Requirements

- Unit tests for enhanced ProcessDocument use case
- Tests for parallel processing scenarios
- Tests for knowledge processing failure handling
- Tests for configuration-based enabling/disabling
- Integration tests with both search and knowledge workflows
- Performance tests for parallel processing

## Dependencies / Preconditions

- Knowledge graph domain entities (Task 0067)
- Knowledge extraction use cases (Task 0068)
- Existing document processing pipeline
- Database migration support

## Implementation Notes

### Parallel Processing Strategy

- Use CompletableFuture for parallel execution
- Ensure search processing is not blocked by knowledge processing
- Implement proper error isolation between workflows
- Consider resource allocation for concurrent processing

### Error Handling Strategy

- Knowledge processing errors should not fail the entire document processing
- Collect and preserve warnings from knowledge extraction
- Provide clear error messages for troubleshooting
- Support retry mechanisms for transient failures

### Configuration Strategy

- Allow runtime configuration changes
- Support per-document-type settings
- Provide sensible defaults for new installations
- Consider performance impact of enabled features
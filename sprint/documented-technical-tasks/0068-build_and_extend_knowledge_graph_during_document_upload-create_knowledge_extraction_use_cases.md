# Create Knowledge Extraction Use Cases

## Related User Story

User Story: build_and_extend_knowledge_graph_during_document_upload

## Objective

Create use cases for extracting knowledge from documents and building/extending the knowledge graph, following Clean Architecture principles and ensuring the knowledge extraction process integrates seamlessly with the existing document processing pipeline.

## Scope

- Create ExtractKnowledgeFromDocument use case
- Create BuildKnowledgeGraph use case  
- Create ExtendExistingKnowledgeGraph use case
- Create ValidateKnowledgeQuality use case
- Create input/output models for knowledge extraction
- Define interfaces for knowledge extraction services
- Ensure use cases are framework-agnostic and testable

## Out of Scope

- Actual knowledge extraction algorithms (belongs in infrastructure)
- Neo4j-specific implementation details
- REST API integration (belongs in interface adapters)
- Frontend integration

## Clean Architecture Placement

usecases

## Execution Dependencies

- 0067-build_and_extend_knowledge_graph_during_document_upload-create_knowledge_graph_domain_entities.md

## Implementation Details

### Use Case Models

**ExtractKnowledgeInput:**
```java
public record ExtractKnowledgeInput(
    String documentId,
    String documentContent,
    String documentTitle,
    String documentType,
    Map<String, Object> extractionOptions
) {
    public ExtractKnowledgeInput {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId cannot be null or blank");
        }
        if (documentContent == null || documentContent.isBlank()) {
            throw new IllegalArgumentException("documentContent cannot be null or blank");
        }
        extractionOptions = Map.copyOf(Objects.requireNonNull(extractionOptions, "extractionOptions cannot be null"));
    }
    
    public boolean hasMinimumContentLength() {
        return documentContent.length() >= 100; // Configurable threshold
    }
    
    public boolean isExtractionEnabled(String extractionType) {
        return extractionOptions.getOrDefault(extractionType + "_enabled", true).equals(true);
    }
}
```

**ExtractKnowledgeOutput:**
```java
public record ExtractKnowledgeOutput(
    ExtractedKnowledge extractedKnowledge,
    KnowledgeExtractionStatus status,
    List<String> warnings,
    List<String> errors,
    Duration processingTime
) {
    public ExtractKnowledgeOutput {
        Objects.requireNonNull(extractedKnowledge, "extractedKnowledge cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings cannot be null"));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors cannot be null"));
        Objects.requireNonNull(processingTime, "processingTime cannot be null");
    }
    
    public boolean isSuccessful() {
        return status == KnowledgeExtractionStatus.SUCCESS || status == KnowledgeExtractionStatus.PARTIAL_SUCCESS;
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
```

**KnowledgeExtractionStatus Enum:**
```java
public enum KnowledgeExtractionStatus {
    SUCCESS("Knowledge extraction completed successfully"),
    PARTIAL_SUCCESS("Knowledge extraction completed with warnings"),
    FAILED("Knowledge extraction failed"),
    INSUFFICIENT_CONTENT("Document content insufficient for knowledge extraction"),
    UNSUPPORTED_FORMAT("Document format not supported for knowledge extraction"),
    PROCESSING_ERROR("Error occurred during knowledge processing");
    
    private final String description;
    
    KnowledgeExtractionStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
```

### Core Use Cases

**ExtractKnowledgeFromDocument Use Case:**
```java
@ApplicationScoped
public final class ExtractKnowledgeFromDocument {
    
    private final KnowledgeExtractionService knowledgeExtractionService;
    private final DocumentQualityValidator documentQualityValidator;
    private final Clock clock;
    
    public ExtractKnowledgeFromDocument(
        KnowledgeExtractionService knowledgeExtractionService,
        DocumentQualityValidator documentQualityValidator,
        Clock clock
    ) {
        this.knowledgeExtractionService = Objects.requireNonNull(knowledgeExtractionService, "knowledgeExtractionService cannot be null");
        this.documentQualityValidator = Objects.requireNonNull(documentQualityValidator, "documentQualityValidator cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }
    
    public ExtractKnowledgeOutput execute(ExtractKnowledgeInput input) {
        Objects.requireNonNull(input, "input cannot be null");
        
        Instant startTime = clock.instant();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try {
            // Validate document quality
            DocumentQualityResult qualityResult = documentQualityValidator.validateForKnowledgeExtraction(
                input.documentContent(), 
                input.documentType()
            );
            
            if (!qualityResult.isSufficientForExtraction()) {
                return createFailureOutput(
                    KnowledgeExtractionStatus.INSUFFICIENT_CONTENT,
                    qualityResult.getIssues(),
                    startTime
                );
            }
            
            warnings.addAll(qualityResult.getWarnings());
            
            // Extract knowledge using the service
            ExtractedKnowledge extractedKnowledge = knowledgeExtractionService.extractKnowledge(
                input.documentContent(),
                input.documentTitle(),
                input.documentType(),
                input.extractionOptions()
            );
            
            // Determine status based on results
            KnowledgeExtractionStatus status = determineStatus(extractedKnowledge, warnings);
            
            Duration processingTime = Duration.between(startTime, clock.instant());
            
            return new ExtractKnowledgeOutput(
                extractedKnowledge,
                status,
                warnings,
                errors,
                processingTime
            );
            
        } catch (UnsupportedDocumentFormatException e) {
            return createFailureOutput(
                KnowledgeExtractionStatus.UNSUPPORTED_FORMAT,
                List.of(e.getMessage()),
                startTime
            );
        } catch (Exception e) {
            return createFailureOutput(
                KnowledgeExtractionStatus.PROCESSING_ERROR,
                List.of("Unexpected error during knowledge extraction: " + e.getMessage()),
                startTime
            );
        }
    }
    
    private KnowledgeExtractionStatus determineStatus(ExtractedKnowledge knowledge, List<String> warnings) {
        if (knowledge.isEmpty()) {
            return KnowledgeExtractionStatus.INSUFFICIENT_CONTENT;
        }
        
        if (!warnings.isEmpty()) {
            return KnowledgeExtractionStatus.PARTIAL_SUCCESS;
        }
        
        return KnowledgeExtractionStatus.SUCCESS;
    }
    
    private ExtractKnowledgeOutput createFailureOutput(
        KnowledgeExtractionStatus status, 
        List<String> errors, 
        Instant startTime
    ) {
        return new ExtractKnowledgeOutput(
            new ExtractedKnowledge(List.of(), List.of(), null, createEmptyMetadata()),
            status,
            List.of(),
            errors,
            Duration.between(startTime, clock.instant())
        );
    }
}
```

**BuildKnowledgeGraph Use Case:**
```java
@ApplicationScoped
public final class BuildKnowledgeGraph {
    
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final KnowledgeGraphDomainService knowledgeGraphDomainService;
    
    public BuildKnowledgeGraph(
        KnowledgeGraphRepository knowledgeGraphRepository,
        KnowledgeGraphDomainService knowledgeGraphDomainService
    ) {
        this.knowledgeGraphRepository = Objects.requireNonNull(knowledgeGraphRepository, "knowledgeGraphRepository cannot be null");
        this.knowledgeGraphDomainService = Objects.requireNonNull(knowledgeGraphDomainService, "knowledgeGraphDomainService cannot be null");
    }
    
    public BuildKnowledgeGraphOutput execute(BuildKnowledgeGraphInput input) {
        Objects.requireNonNull(input, "input cannot be null");
        
        try {
            // Check if knowledge graph already exists
            Optional<KnowledgeGraph> existingGraph = knowledgeGraphRepository.findByName(input.graphName());
            
            KnowledgeGraph resultGraph;
            if (existingGraph.isPresent()) {
                // Extend existing graph
                resultGraph = knowledgeGraphDomainService.mergeExtractedKnowledge(
                    existingGraph.get(),
                    input.extractedKnowledge()
                );
            } else {
                // Create new graph
                resultGraph = createNewGraph(input.graphName(), input.extractedKnowledge());
            }
            
            // Validate graph consistency
            knowledgeGraphDomainService.validateGraphConsistency(resultGraph);
            
            // Save the graph
            KnowledgeGraph savedGraph = knowledgeGraphRepository.save(resultGraph);
            
            return BuildKnowledgeGraphOutput.success(
                savedGraph.getGraphId(),
                savedGraph.getNodes().size(),
                savedGraph.getRelationships().size(),
                existingGraph.isPresent()
            );
            
        } catch (Exception e) {
            return BuildKnowledgeGraphOutput.failure(
                "Failed to build knowledge graph: " + e.getMessage()
            );
        }
    }
    
    private KnowledgeGraph createNewGraph(String graphName, ExtractedKnowledge extractedKnowledge) {
        return new KnowledgeGraph(
            GraphId.generate(),
            graphName,
            new HashSet<>(extractedKnowledge.nodes()),
            new HashSet<>(extractedKnowledge.relationships()),
            new GraphMetadata(extractedKnowledge.metadata()),
            Instant.now(),
            Instant.now()
        );
    }
}
```

### Service Interfaces

**KnowledgeExtractionService Interface:**
```java
public interface KnowledgeExtractionService {
    
    ExtractedKnowledge extractKnowledge(
        String documentContent,
        String documentTitle,
        String documentType,
        Map<String, Object> extractionOptions
    ) throws KnowledgeExtractionException;
    
    boolean supportsDocumentType(String documentType);
    
    List<String> getSupportedExtractionTypes();
    
    Map<String, Object> getDefaultExtractionOptions();
}
```

**DocumentQualityValidator Interface:**
```java
public interface DocumentQualityValidator {
    
    DocumentQualityResult validateForKnowledgeExtraction(String content, String documentType);
    
    boolean hasMinimumContentLength(String content);
    
    boolean hasAcceptableLanguage(String content);
    
    boolean hasStructuredContent(String content, String documentType);
}
```

**KnowledgeGraphRepository Interface:**
```java
public interface KnowledgeGraphRepository {
    
    KnowledgeGraph save(KnowledgeGraph knowledgeGraph);
    
    Optional<KnowledgeGraph> findById(GraphId graphId);
    
    Optional<KnowledgeGraph> findByName(String name);
    
    List<KnowledgeGraph> findAll();
    
    void delete(GraphId graphId);
    
    boolean existsByName(String name);
    
    // Query methods for graph traversal
    List<KnowledgeNode> findNodesConnectedTo(NodeId nodeId);
    
    List<KnowledgeRelationship> findRelationshipsByType(RelationshipType type);
    
    KnowledgeGraph findSubgraphAroundNode(NodeId nodeId, int depth);
}
```

### Additional Models

**BuildKnowledgeGraphInput:**
```java
public record BuildKnowledgeGraphInput(
    String graphName,
    ExtractedKnowledge extractedKnowledge,
    boolean allowMerging
) {
    public BuildKnowledgeGraphInput {
        if (graphName == null || graphName.isBlank()) {
            throw new IllegalArgumentException("graphName cannot be null or blank");
        }
        Objects.requireNonNull(extractedKnowledge, "extractedKnowledge cannot be null");
    }
}
```

**BuildKnowledgeGraphOutput:**
```java
public record BuildKnowledgeGraphOutput(
    GraphId graphId,
    int totalNodes,
    int totalRelationships,
    boolean wasExtended,
    boolean success,
    String errorMessage
) {
    public static BuildKnowledgeGraphOutput success(
        GraphId graphId, 
        int totalNodes, 
        int totalRelationships, 
        boolean wasExtended
    ) {
        return new BuildKnowledgeGraphOutput(
            graphId, totalNodes, totalRelationships, wasExtended, true, null
        );
    }
    
    public static BuildKnowledgeGraphOutput failure(String errorMessage) {
        return new BuildKnowledgeGraphOutput(
            null, 0, 0, false, false, errorMessage
        );
    }
}
```

## Files / Modules Impacted

- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/ExtractKnowledgeFromDocument.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/BuildKnowledgeGraph.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/ExtendExistingKnowledgeGraph.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/ExtractKnowledgeInput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/ExtractKnowledgeOutput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/BuildKnowledgeGraphInput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/BuildKnowledgeGraphOutput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/KnowledgeExtractionStatus.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/interfaces/knowledge/KnowledgeExtractionService.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/interfaces/knowledge/DocumentQualityValidator.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/interfaces/knowledge/KnowledgeGraphRepository.java`

## Expected Behavior

**Given** a document is uploaded with sufficient content quality  
**When** knowledge extraction is performed  
**Then** the system should extract entities and relationships and return them in a structured format

**Given** extracted knowledge contains entities that already exist in the graph  
**When** the knowledge graph is built or extended  
**Then** the system should merge similar entities rather than create duplicates

**Given** a document has insufficient content for knowledge extraction  
**When** the extraction process runs  
**Then** the system should return an appropriate status with clear warnings

## Acceptance Criteria

- ✅ Use cases follow Clean Architecture principles (no framework dependencies)
- ✅ Knowledge extraction handles various document types and quality levels
- ✅ Graph building supports both new graph creation and existing graph extension
- ✅ Error handling provides clear feedback for different failure scenarios
- ✅ Use cases are testable with mock implementations
- ✅ Input validation prevents invalid operations
- ✅ Processing time is tracked for monitoring purposes

## Testing Requirements

- Unit tests for all use cases with mock dependencies
- Tests for various document quality scenarios
- Tests for knowledge merging and deduplication logic
- Tests for error handling and edge cases
- Integration tests with real knowledge extraction services
- Performance tests for large documents and graphs

## Dependencies / Preconditions

- Knowledge graph domain entities (Task 0067)
- Understanding of knowledge extraction algorithms
- Clean Architecture structure in place

## Implementation Notes

### Knowledge Extraction Strategy

- Support multiple extraction algorithms (NLP, ML-based, rule-based)
- Allow configurable extraction options per document type
- Implement quality thresholds to avoid low-quality extractions
- Support incremental processing for large documents

### Graph Building Strategy

- Use similarity algorithms to detect duplicate entities
- Implement confidence-based merging decisions
- Preserve provenance information for traceability
- Support rollback in case of processing failures

### Error Handling

- Distinguish between recoverable and non-recoverable errors
- Provide actionable error messages for users
- Log detailed error information for debugging
- Support partial success scenarios with warnings
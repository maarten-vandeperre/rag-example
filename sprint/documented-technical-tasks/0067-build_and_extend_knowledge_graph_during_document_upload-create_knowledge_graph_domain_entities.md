# Create Knowledge Graph Domain Entities

## Related User Story

User Story: build_and_extend_knowledge_graph_during_document_upload

## Objective

Create domain entities and value objects for representing knowledge graph concepts within the Clean Architecture, enabling the system to model knowledge extraction and graph relationships from uploaded documents.

## Scope

- Create core knowledge graph domain entities (KnowledgeNode, KnowledgeRelationship, KnowledgeGraph)
- Create value objects for knowledge metadata (NodeType, RelationshipType, ConfidenceScore)
- Create knowledge extraction result models
- Define domain services for knowledge graph operations
- Ensure all entities follow Clean Architecture principles (pure Java, no framework dependencies)

## Out of Scope

- Persistence implementation (belongs in infrastructure layer)
- Knowledge extraction algorithms (will be separate use cases)
- Neo4j-specific models or annotations
- REST API models (belong in interface adapters)

## Clean Architecture Placement

domain

## Execution Dependencies

None

## Implementation Details

### Core Domain Entities

**KnowledgeNode Entity:**
```java
public final class KnowledgeNode {
    private final NodeId nodeId;
    private final String label;
    private final NodeType nodeType;
    private final Map<String, Object> properties;
    private final DocumentReference sourceDocument;
    private final ConfidenceScore confidence;
    private final Instant createdAt;
    private final Instant lastUpdatedAt;
    
    // Constructor with validation
    // Business methods for node operations
    // Immutable design with builder pattern for updates
}
```

**KnowledgeRelationship Entity:**
```java
public final class KnowledgeRelationship {
    private final RelationshipId relationshipId;
    private final NodeId fromNodeId;
    private final NodeId toNodeId;
    private final RelationshipType relationshipType;
    private final Map<String, Object> properties;
    private final DocumentReference sourceDocument;
    private final ConfidenceScore confidence;
    private final Instant createdAt;
    
    // Constructor with validation
    // Business methods for relationship operations
}
```

**KnowledgeGraph Entity:**
```java
public final class KnowledgeGraph {
    private final GraphId graphId;
    private final String name;
    private final Set<KnowledgeNode> nodes;
    private final Set<KnowledgeRelationship> relationships;
    private final GraphMetadata metadata;
    private final Instant createdAt;
    private final Instant lastUpdatedAt;
    
    // Methods for adding/removing nodes and relationships
    // Methods for graph traversal and querying
    // Methods for merging with other graphs
}
```

### Value Objects

**NodeId Value Object:**
```java
public record NodeId(String value) {
    public NodeId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("NodeId cannot be null or blank");
        }
        // Additional validation for format
    }
    
    public static NodeId generate() {
        return new NodeId(UUID.randomUUID().toString());
    }
}
```

**NodeType Value Object:**
```java
public enum NodeType {
    CONCEPT("Concept"),
    ENTITY("Entity"), 
    PERSON("Person"),
    ORGANIZATION("Organization"),
    LOCATION("Location"),
    EVENT("Event"),
    DOCUMENT_SECTION("DocumentSection"),
    TOPIC("Topic"),
    KEYWORD("Keyword");
    
    private final String displayName;
    
    NodeType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

**RelationshipType Value Object:**
```java
public enum RelationshipType {
    RELATED_TO("Related To"),
    PART_OF("Part Of"),
    MENTIONS("Mentions"),
    DEFINED_IN("Defined In"),
    SIMILAR_TO("Similar To"),
    DEPENDS_ON("Depends On"),
    CONTAINS("Contains"),
    REFERENCES("References"),
    DERIVED_FROM("Derived From");
    
    private final String displayName;
    
    RelationshipType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

**ConfidenceScore Value Object:**
```java
public record ConfidenceScore(double value) {
    public ConfidenceScore {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("Confidence score must be between 0.0 and 1.0");
        }
    }
    
    public static ConfidenceScore high() {
        return new ConfidenceScore(0.9);
    }
    
    public static ConfidenceScore medium() {
        return new ConfidenceScore(0.7);
    }
    
    public static ConfidenceScore low() {
        return new ConfidenceScore(0.5);
    }
    
    public boolean isHighConfidence() {
        return value >= 0.8;
    }
    
    public boolean isMediumConfidence() {
        return value >= 0.6 && value < 0.8;
    }
    
    public boolean isLowConfidence() {
        return value < 0.6;
    }
}
```

### Knowledge Extraction Models

**ExtractedKnowledge Value Object:**
```java
public record ExtractedKnowledge(
    List<KnowledgeNode> nodes,
    List<KnowledgeRelationship> relationships,
    DocumentReference sourceDocument,
    ExtractionMetadata metadata
) {
    public ExtractedKnowledge {
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes cannot be null"));
        relationships = List.copyOf(Objects.requireNonNull(relationships, "relationships cannot be null"));
        Objects.requireNonNull(sourceDocument, "sourceDocument cannot be null");
        Objects.requireNonNull(metadata, "metadata cannot be null");
    }
    
    public boolean isEmpty() {
        return nodes.isEmpty() && relationships.isEmpty();
    }
    
    public int getTotalElements() {
        return nodes.size() + relationships.size();
    }
}
```

**ExtractionMetadata Value Object:**
```java
public record ExtractionMetadata(
    String extractionMethod,
    Instant extractedAt,
    Duration processingTime,
    Map<String, Object> algorithmParameters,
    List<String> warnings
) {
    public ExtractionMetadata {
        if (extractionMethod == null || extractionMethod.isBlank()) {
            throw new IllegalArgumentException("extractionMethod cannot be null or blank");
        }
        Objects.requireNonNull(extractedAt, "extractedAt cannot be null");
        Objects.requireNonNull(processingTime, "processingTime cannot be null");
        algorithmParameters = Map.copyOf(Objects.requireNonNull(algorithmParameters, "algorithmParameters cannot be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings cannot be null"));
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
```

### Domain Services

**KnowledgeGraphDomainService:**
```java
public final class KnowledgeGraphDomainService {
    
    public KnowledgeGraph mergeExtractedKnowledge(
        KnowledgeGraph existingGraph, 
        ExtractedKnowledge extractedKnowledge
    ) {
        // Business logic for merging new knowledge with existing graph
        // Handle duplicate detection and knowledge extension
        // Return updated graph
    }
    
    public boolean shouldMergeNodes(KnowledgeNode existing, KnowledgeNode candidate) {
        // Business rules for determining if nodes should be merged
        // Based on similarity, type, properties, etc.
    }
    
    public KnowledgeNode mergeNodes(KnowledgeNode primary, KnowledgeNode secondary) {
        // Business logic for combining node properties and metadata
    }
    
    public void validateGraphConsistency(KnowledgeGraph graph) {
        // Validate that all relationships reference existing nodes
        // Check for circular dependencies where inappropriate
        // Validate confidence scores and metadata
    }
}
```

## Files / Modules Impacted

- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/entities/KnowledgeNode.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/entities/KnowledgeRelationship.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/entities/KnowledgeGraph.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/NodeId.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/RelationshipId.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/GraphId.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/NodeType.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/RelationshipType.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/ConfidenceScore.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/ExtractedKnowledge.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/ExtractionMetadata.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/services/KnowledgeGraphDomainService.java`

## Expected Behavior

**Given** a knowledge extraction process identifies entities and relationships in a document  
**When** the domain models are created  
**Then** they should represent the knowledge in a structured, validated format

**Given** existing knowledge exists in the graph  
**When** new knowledge is extracted from a document  
**Then** the domain service should determine how to merge or extend existing knowledge

**Given** knowledge nodes and relationships are created  
**When** they are validated  
**Then** they should enforce business rules for consistency and quality

## Acceptance Criteria

- ✅ All domain entities are pure Java with no framework dependencies
- ✅ Value objects enforce validation rules and immutability
- ✅ Domain services contain business logic for knowledge graph operations
- ✅ Entities support knowledge merging and extension scenarios
- ✅ Models support confidence scoring and metadata tracking
- ✅ All classes follow Clean Architecture principles
- ✅ Code compiles and passes unit tests

## Testing Requirements

- Unit tests for all domain entities and value objects
- Unit tests for domain service business logic
- Tests for knowledge merging scenarios
- Tests for validation rules and edge cases
- Tests for confidence score calculations
- Performance tests for large knowledge graphs

## Dependencies / Preconditions

- Existing Clean Architecture structure
- Java 25 development environment
- Understanding of knowledge graph concepts

## Implementation Notes

### Design Principles

- **Immutability**: All entities and value objects should be immutable
- **Validation**: Enforce business rules at construction time
- **Pure Domain**: No dependencies on infrastructure or frameworks
- **Rich Domain Model**: Include business behavior, not just data

### Knowledge Merging Strategy

- Use similarity algorithms to detect duplicate nodes
- Merge properties by combining and deduplicating
- Preserve source document references for traceability
- Update confidence scores based on multiple sources

### Performance Considerations

- Use efficient data structures for large graphs
- Consider lazy loading for graph traversal
- Optimize for common query patterns
- Design for eventual consistency in distributed scenarios
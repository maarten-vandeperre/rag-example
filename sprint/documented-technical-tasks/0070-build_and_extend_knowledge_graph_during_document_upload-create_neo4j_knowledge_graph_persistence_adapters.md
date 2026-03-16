# Create Neo4j Knowledge Graph Persistence Adapters

## Related User Story

User Story: build_and_extend_knowledge_graph_during_document_upload

## Objective

Implement Neo4j-based persistence adapters for the knowledge graph repository interfaces, providing efficient storage and retrieval of knowledge graphs, nodes, and relationships while maintaining Clean Architecture boundaries.

## Scope

- Implement Neo4jKnowledgeGraphRepository
- Create Neo4j entity mappers for domain models
- Implement graph traversal and query operations
- Add Neo4j-specific configuration and connection management
- Create Cypher query builders for complex operations
- Implement transaction management for graph operations

## Out of Scope

- Neo4j installation and setup (handled in infrastructure tasks)
- Domain model changes (already defined in previous tasks)
- REST API integration (belongs in interface adapters)
- Frontend integration

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0067-build_and_extend_knowledge_graph_during_document_upload-create_knowledge_graph_domain_entities.md
- 0068-build_and_extend_knowledge_graph_during_document_upload-create_knowledge_extraction_use_cases.md

## Implementation Details

### Neo4j Repository Implementation

**Neo4jKnowledgeGraphRepository:**
```java
@ApplicationScoped
public final class Neo4jKnowledgeGraphRepository implements KnowledgeGraphRepository {
    
    private final Driver neo4jDriver;
    private final KnowledgeGraphMapper knowledgeGraphMapper;
    private final KnowledgeNodeMapper nodeMapper;
    private final KnowledgeRelationshipMapper relationshipMapper;
    
    public Neo4jKnowledgeGraphRepository(
        Driver neo4jDriver,
        KnowledgeGraphMapper knowledgeGraphMapper,
        KnowledgeNodeMapper nodeMapper,
        KnowledgeRelationshipMapper relationshipMapper
    ) {
        this.neo4jDriver = Objects.requireNonNull(neo4jDriver, "neo4jDriver cannot be null");
        this.knowledgeGraphMapper = Objects.requireNonNull(knowledgeGraphMapper, "knowledgeGraphMapper cannot be null");
        this.nodeMapper = Objects.requireNonNull(nodeMapper, "nodeMapper cannot be null");
        this.relationshipMapper = Objects.requireNonNull(relationshipMapper, "relationshipMapper cannot be null");
    }
    
    @Override
    public KnowledgeGraph save(KnowledgeGraph knowledgeGraph) {
        Objects.requireNonNull(knowledgeGraph, "knowledgeGraph cannot be null");
        
        try (Session session = neo4jDriver.session()) {
            return session.executeWrite(tx -> {
                // Save or update the graph metadata
                saveGraphMetadata(tx, knowledgeGraph);
                
                // Save all nodes
                for (KnowledgeNode node : knowledgeGraph.getNodes()) {
                    saveNode(tx, node, knowledgeGraph.getGraphId());
                }
                
                // Save all relationships
                for (KnowledgeRelationship relationship : knowledgeGraph.getRelationships()) {
                    saveRelationship(tx, relationship, knowledgeGraph.getGraphId());
                }
                
                return knowledgeGraph;
            });
        } catch (Exception e) {
            throw new RepositoryException("Failed to save knowledge graph: " + knowledgeGraph.getGraphId(), e);
        }
    }
    
    @Override
    public Optional<KnowledgeGraph> findById(GraphId graphId) {
        Objects.requireNonNull(graphId, "graphId cannot be null");
        
        try (Session session = neo4jDriver.session()) {
            return session.executeRead(tx -> {
                // Find graph metadata
                Result graphResult = tx.run(
                    "MATCH (g:Graph {graphId: $graphId}) RETURN g",
                    Map.of("graphId", graphId.value())
                );
                
                if (!graphResult.hasNext()) {
                    return Optional.empty();
                }
                
                Record graphRecord = graphResult.next();
                
                // Find all nodes in the graph
                Result nodesResult = tx.run(
                    "MATCH (n:KnowledgeNode {graphId: $graphId}) RETURN n",
                    Map.of("graphId", graphId.value())
                );
                
                Set<KnowledgeNode> nodes = new HashSet<>();
                while (nodesResult.hasNext()) {
                    Record nodeRecord = nodesResult.next();
                    nodes.add(nodeMapper.fromNeo4jNode(nodeRecord.get("n").asNode()));
                }
                
                // Find all relationships in the graph
                Result relationshipsResult = tx.run(
                    "MATCH (a:KnowledgeNode {graphId: $graphId})-[r:KNOWLEDGE_RELATIONSHIP]->(b:KnowledgeNode {graphId: $graphId}) RETURN r, a.nodeId as fromNodeId, b.nodeId as toNodeId",
                    Map.of("graphId", graphId.value())
                );
                
                Set<KnowledgeRelationship> relationships = new HashSet<>();
                while (relationshipsResult.hasNext()) {
                    Record relRecord = relationshipsResult.next();
                    relationships.add(relationshipMapper.fromNeo4jRelationship(
                        relRecord.get("r").asRelationship(),
                        relRecord.get("fromNodeId").asString(),
                        relRecord.get("toNodeId").asString()
                    ));
                }
                
                KnowledgeGraph graph = knowledgeGraphMapper.fromNeo4jRecord(graphRecord, nodes, relationships);
                return Optional.of(graph);
            });
        } catch (Exception e) {
            throw new RepositoryException("Failed to find knowledge graph: " + graphId, e);
        }
    }
    
    @Override
    public Optional<KnowledgeGraph> findByName(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        
        try (Session session = neo4jDriver.session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(
                    "MATCH (g:Graph {name: $name}) RETURN g.graphId as graphId",
                    Map.of("name", name)
                );
                
                if (!result.hasNext()) {
                    return Optional.empty();
                }
                
                String graphId = result.next().get("graphId").asString();
                return findById(new GraphId(graphId));
            });
        } catch (Exception e) {
            throw new RepositoryException("Failed to find knowledge graph by name: " + name, e);
        }
    }
    
    @Override
    public List<KnowledgeNode> findNodesConnectedTo(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "nodeId cannot be null");
        
        try (Session session = neo4jDriver.session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(
                    "MATCH (n:KnowledgeNode {nodeId: $nodeId})-[:KNOWLEDGE_RELATIONSHIP]-(connected:KnowledgeNode) RETURN DISTINCT connected",
                    Map.of("nodeId", nodeId.value())
                );
                
                List<KnowledgeNode> connectedNodes = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    connectedNodes.add(nodeMapper.fromNeo4jNode(record.get("connected").asNode()));
                }
                
                return connectedNodes;
            });
        } catch (Exception e) {
            throw new RepositoryException("Failed to find nodes connected to: " + nodeId, e);
        }
    }
    
    @Override
    public List<KnowledgeRelationship> findRelationshipsByType(RelationshipType type) {
        Objects.requireNonNull(type, "type cannot be null");
        
        try (Session session = neo4jDriver.session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(
                    "MATCH (a:KnowledgeNode)-[r:KNOWLEDGE_RELATIONSHIP {relationshipType: $type}]->(b:KnowledgeNode) " +
                    "RETURN r, a.nodeId as fromNodeId, b.nodeId as toNodeId",
                    Map.of("type", type.name())
                );
                
                List<KnowledgeRelationship> relationships = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    relationships.add(relationshipMapper.fromNeo4jRelationship(
                        record.get("r").asRelationship(),
                        record.get("fromNodeId").asString(),
                        record.get("toNodeId").asString()
                    ));
                }
                
                return relationships;
            });
        } catch (Exception e) {
            throw new RepositoryException("Failed to find relationships by type: " + type, e);
        }
    }
    
    @Override
    public KnowledgeGraph findSubgraphAroundNode(NodeId nodeId, int depth) {
        Objects.requireNonNull(nodeId, "nodeId cannot be null");
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be at least 1");
        }
        
        try (Session session = neo4jDriver.session()) {
            return session.executeRead(tx -> {
                // Find nodes within the specified depth
                Result nodesResult = tx.run(
                    "MATCH (start:KnowledgeNode {nodeId: $nodeId}) " +
                    "CALL apoc.path.subgraphNodes(start, {maxLevel: $depth}) YIELD node " +
                    "RETURN DISTINCT node",
                    Map.of("nodeId", nodeId.value(), "depth", depth)
                );
                
                Set<KnowledgeNode> nodes = new HashSet<>();
                Set<String> nodeIds = new HashSet<>();
                
                while (nodesResult.hasNext()) {
                    Record record = nodesResult.next();
                    KnowledgeNode node = nodeMapper.fromNeo4jNode(record.get("node").asNode());
                    nodes.add(node);
                    nodeIds.add(node.getNodeId().value());
                }
                
                // Find relationships between these nodes
                Result relationshipsResult = tx.run(
                    "MATCH (a:KnowledgeNode)-[r:KNOWLEDGE_RELATIONSHIP]->(b:KnowledgeNode) " +
                    "WHERE a.nodeId IN $nodeIds AND b.nodeId IN $nodeIds " +
                    "RETURN r, a.nodeId as fromNodeId, b.nodeId as toNodeId",
                    Map.of("nodeIds", nodeIds)
                );
                
                Set<KnowledgeRelationship> relationships = new HashSet<>();
                while (relationshipsResult.hasNext()) {
                    Record record = relationshipsResult.next();
                    relationships.add(relationshipMapper.fromNeo4jRelationship(
                        record.get("r").asRelationship(),
                        record.get("fromNodeId").asString(),
                        record.get("toNodeId").asString()
                    ));
                }
                
                // Create subgraph
                return new KnowledgeGraph(
                    GraphId.generate(),
                    "subgraph-" + nodeId.value(),
                    nodes,
                    relationships,
                    new GraphMetadata("Subgraph around " + nodeId.value()),
                    Instant.now(),
                    Instant.now()
                );
            });
        } catch (Exception e) {
            throw new RepositoryException("Failed to find subgraph around node: " + nodeId, e);
        }
    }
    
    private void saveGraphMetadata(Transaction tx, KnowledgeGraph graph) {
        tx.run(
            "MERGE (g:Graph {graphId: $graphId}) " +
            "SET g.name = $name, g.createdAt = $createdAt, g.lastUpdatedAt = $lastUpdatedAt",
            Map.of(
                "graphId", graph.getGraphId().value(),
                "name", graph.getName(),
                "createdAt", graph.getCreatedAt().toString(),
                "lastUpdatedAt", graph.getLastUpdatedAt().toString()
            )
        );
    }
    
    private void saveNode(Transaction tx, KnowledgeNode node, GraphId graphId) {
        Map<String, Object> properties = new HashMap<>(node.getProperties());
        properties.put("nodeId", node.getNodeId().value());
        properties.put("label", node.getLabel());
        properties.put("nodeType", node.getNodeType().name());
        properties.put("graphId", graphId.value());
        properties.put("confidence", node.getConfidence().value());
        properties.put("createdAt", node.getCreatedAt().toString());
        properties.put("lastUpdatedAt", node.getLastUpdatedAt().toString());
        
        if (node.getSourceDocument() != null) {
            properties.put("sourceDocumentId", node.getSourceDocument().documentId().toString());
        }
        
        tx.run(
            "MERGE (n:KnowledgeNode {nodeId: $nodeId}) SET n += $properties",
            Map.of("nodeId", node.getNodeId().value(), "properties", properties)
        );
    }
    
    private void saveRelationship(Transaction tx, KnowledgeRelationship relationship, GraphId graphId) {
        Map<String, Object> properties = new HashMap<>(relationship.getProperties());
        properties.put("relationshipId", relationship.getRelationshipId().value());
        properties.put("relationshipType", relationship.getRelationshipType().name());
        properties.put("confidence", relationship.getConfidence().value());
        properties.put("createdAt", relationship.getCreatedAt().toString());
        
        if (relationship.getSourceDocument() != null) {
            properties.put("sourceDocumentId", relationship.getSourceDocument().documentId().toString());
        }
        
        tx.run(
            "MATCH (a:KnowledgeNode {nodeId: $fromNodeId}), (b:KnowledgeNode {nodeId: $toNodeId}) " +
            "MERGE (a)-[r:KNOWLEDGE_RELATIONSHIP {relationshipId: $relationshipId}]->(b) " +
            "SET r += $properties",
            Map.of(
                "fromNodeId", relationship.getFromNodeId().value(),
                "toNodeId", relationship.getToNodeId().value(),
                "relationshipId", relationship.getRelationshipId().value(),
                "properties", properties
            )
        );
    }
}
```

### Entity Mappers

**KnowledgeNodeMapper:**
```java
@ApplicationScoped
public final class KnowledgeNodeMapper {
    
    public KnowledgeNode fromNeo4jNode(Node neo4jNode) {
        Map<String, Object> properties = neo4jNode.asMap();
        
        NodeId nodeId = new NodeId((String) properties.get("nodeId"));
        String label = (String) properties.get("label");
        NodeType nodeType = NodeType.valueOf((String) properties.get("nodeType"));
        ConfidenceScore confidence = new ConfidenceScore((Double) properties.get("confidence"));
        Instant createdAt = Instant.parse((String) properties.get("createdAt"));
        Instant lastUpdatedAt = Instant.parse((String) properties.get("lastUpdatedAt"));
        
        // Extract custom properties (excluding system properties)
        Map<String, Object> customProperties = new HashMap<>(properties);
        customProperties.remove("nodeId");
        customProperties.remove("label");
        customProperties.remove("nodeType");
        customProperties.remove("confidence");
        customProperties.remove("createdAt");
        customProperties.remove("lastUpdatedAt");
        customProperties.remove("graphId");
        customProperties.remove("sourceDocumentId");
        
        // Create document reference if available
        DocumentReference sourceDocument = null;
        if (properties.containsKey("sourceDocumentId")) {
            String docId = (String) properties.get("sourceDocumentId");
            sourceDocument = new DocumentReference(UUID.fromString(docId));
        }
        
        return new KnowledgeNode(
            nodeId,
            label,
            nodeType,
            customProperties,
            sourceDocument,
            confidence,
            createdAt,
            lastUpdatedAt
        );
    }
    
    public Map<String, Object> toNeo4jProperties(KnowledgeNode node, GraphId graphId) {
        Map<String, Object> properties = new HashMap<>(node.getProperties());
        properties.put("nodeId", node.getNodeId().value());
        properties.put("label", node.getLabel());
        properties.put("nodeType", node.getNodeType().name());
        properties.put("graphId", graphId.value());
        properties.put("confidence", node.getConfidence().value());
        properties.put("createdAt", node.getCreatedAt().toString());
        properties.put("lastUpdatedAt", node.getLastUpdatedAt().toString());
        
        if (node.getSourceDocument() != null) {
            properties.put("sourceDocumentId", node.getSourceDocument().documentId().toString());
        }
        
        return properties;
    }
}
```

**KnowledgeRelationshipMapper:**
```java
@ApplicationScoped
public final class KnowledgeRelationshipMapper {
    
    public KnowledgeRelationship fromNeo4jRelationship(
        Relationship neo4jRelationship, 
        String fromNodeId, 
        String toNodeId
    ) {
        Map<String, Object> properties = neo4jRelationship.asMap();
        
        RelationshipId relationshipId = new RelationshipId((String) properties.get("relationshipId"));
        RelationshipType relationshipType = RelationshipType.valueOf((String) properties.get("relationshipType"));
        ConfidenceScore confidence = new ConfidenceScore((Double) properties.get("confidence"));
        Instant createdAt = Instant.parse((String) properties.get("createdAt"));
        
        // Extract custom properties
        Map<String, Object> customProperties = new HashMap<>(properties);
        customProperties.remove("relationshipId");
        customProperties.remove("relationshipType");
        customProperties.remove("confidence");
        customProperties.remove("createdAt");
        customProperties.remove("sourceDocumentId");
        
        // Create document reference if available
        DocumentReference sourceDocument = null;
        if (properties.containsKey("sourceDocumentId")) {
            String docId = (String) properties.get("sourceDocumentId");
            sourceDocument = new DocumentReference(UUID.fromString(docId));
        }
        
        return new KnowledgeRelationship(
            relationshipId,
            new NodeId(fromNodeId),
            new NodeId(toNodeId),
            relationshipType,
            customProperties,
            sourceDocument,
            confidence,
            createdAt
        );
    }
    
    public Map<String, Object> toNeo4jProperties(KnowledgeRelationship relationship) {
        Map<String, Object> properties = new HashMap<>(relationship.getProperties());
        properties.put("relationshipId", relationship.getRelationshipId().value());
        properties.put("relationshipType", relationship.getRelationshipType().name());
        properties.put("confidence", relationship.getConfidence().value());
        properties.put("createdAt", relationship.getCreatedAt().toString());
        
        if (relationship.getSourceDocument() != null) {
            properties.put("sourceDocumentId", relationship.getSourceDocument().documentId().toString());
        }
        
        return properties;
    }
}
```

### Neo4j Configuration

**Neo4jConfiguration:**
```java
@ApplicationScoped
public final class Neo4jConfiguration {
    
    @ConfigProperty(name = "neo4j.uri", defaultValue = "bolt://localhost:7687")
    String neo4jUri;
    
    @ConfigProperty(name = "neo4j.username", defaultValue = "neo4j")
    String neo4jUsername;
    
    @ConfigProperty(name = "neo4j.password", defaultValue = "password")
    String neo4jPassword;
    
    @ConfigProperty(name = "neo4j.database", defaultValue = "neo4j")
    String neo4jDatabase;
    
    @Produces
    @ApplicationScoped
    public Driver createNeo4jDriver() {
        return GraphDatabase.driver(
            neo4jUri,
            AuthTokens.basic(neo4jUsername, neo4jPassword),
            Config.builder()
                .withMaxConnectionLifetime(30, TimeUnit.MINUTES)
                .withMaxConnectionPoolSize(50)
                .withConnectionAcquisitionTimeout(2, TimeUnit.MINUTES)
                .build()
        );
    }
    
    @PreDestroy
    public void closeDriver(@Disposes Driver driver) {
        if (driver != null) {
            driver.close();
        }
    }
}
```

## Files / Modules Impacted

- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/Neo4jKnowledgeGraphRepository.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/mappers/KnowledgeNodeMapper.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/mappers/KnowledgeRelationshipMapper.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/mappers/KnowledgeGraphMapper.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/Neo4jConfiguration.java`
- `backend/shared-kernel/build.gradle` - Add Neo4j driver dependency

## Expected Behavior

**Given** a knowledge graph is saved to Neo4j  
**When** it is retrieved by ID  
**Then** all nodes and relationships should be preserved with their properties

**Given** nodes are connected by relationships  
**When** finding connected nodes  
**Then** the query should return all directly connected nodes

**Given** a subgraph is requested around a node  
**When** specifying a depth  
**Then** all nodes within that depth should be included with their relationships

## Acceptance Criteria

- ✅ Repository implements all interface methods correctly
- ✅ Entity mappers preserve all domain model properties
- ✅ Graph traversal operations work efficiently
- ✅ Transaction management ensures data consistency
- ✅ Configuration supports different Neo4j environments
- ✅ Error handling provides meaningful exception messages
- ✅ Performance is acceptable for expected graph sizes

## Testing Requirements

- Unit tests for repository methods with embedded Neo4j
- Integration tests with real Neo4j instance
- Tests for entity mapping accuracy
- Tests for graph traversal operations
- Performance tests for large graphs
- Tests for transaction rollback scenarios

## Dependencies / Preconditions

- Neo4j database running and accessible
- Neo4j Java driver dependency
- Knowledge graph domain entities
- Repository interfaces defined

## Implementation Notes

### Neo4j Schema Design

- Use labels for node types (KnowledgeNode, Graph)
- Use relationship type KNOWLEDGE_RELATIONSHIP for all knowledge relationships
- Store relationship type as property for flexibility
- Index on nodeId and graphId for performance

### Performance Considerations

- Use MERGE for upsert operations to handle duplicates
- Batch operations when saving large graphs
- Use appropriate indexes for common query patterns
- Consider using APOC procedures for complex operations

### Transaction Management

- Use write transactions for all mutations
- Use read transactions for queries
- Implement proper error handling and rollback
- Consider connection pooling for high throughput
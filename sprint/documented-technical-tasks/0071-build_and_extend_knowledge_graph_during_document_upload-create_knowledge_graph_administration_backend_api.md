# Create Knowledge Graph Administration Backend API

## Related User Story

User Story: build_and_extend_knowledge_graph_during_document_upload

## Objective

Create REST API endpoints for the knowledge graph administration interface, allowing authorized users to browse, search, and explore the knowledge graph that has been built from uploaded documents.

## Scope

- Create REST controllers for knowledge graph operations
- Implement graph browsing and search endpoints
- Add node and relationship detail endpoints
- Create graph visualization data endpoints
- Implement authorization for graph administration features
- Add API models and DTOs for graph data
- Create graph statistics and metrics endpoints

## Out of Scope

- Frontend implementation (handled in separate tasks)
- Knowledge extraction algorithms (already implemented)
- Neo4j implementation details (handled in infrastructure tasks)
- User management features (use existing authorization)

## Clean Architecture Placement

interface adapters

## Execution Dependencies

- 0067-build_and_extend_knowledge_graph_during_document_upload-create_knowledge_graph_domain_entities.md
- 0068-build_and_extend_knowledge_graph_during_document_upload-create_knowledge_extraction_use_cases.md
- 0070-build_and_extend_knowledge_graph_during_document_upload-create_neo4j_knowledge_graph_persistence_adapters.md

## Implementation Details

### REST Controllers

**KnowledgeGraphController:**
```java
@RestController
@RequestMapping("/api/admin/knowledge-graph")
@PreAuthorize("hasRole('ADMIN')")
public final class KnowledgeGraphController {
    
    private final BrowseKnowledgeGraph browseKnowledgeGraph;
    private final SearchKnowledgeGraph searchKnowledgeGraph;
    private final GetKnowledgeGraphStatistics getKnowledgeGraphStatistics;
    private final KnowledgeGraphDtoMapper dtoMapper;
    
    public KnowledgeGraphController(
        BrowseKnowledgeGraph browseKnowledgeGraph,
        SearchKnowledgeGraph searchKnowledgeGraph,
        GetKnowledgeGraphStatistics getKnowledgeGraphStatistics,
        KnowledgeGraphDtoMapper dtoMapper
    ) {
        this.browseKnowledgeGraph = Objects.requireNonNull(browseKnowledgeGraph, "browseKnowledgeGraph cannot be null");
        this.searchKnowledgeGraph = Objects.requireNonNull(searchKnowledgeGraph, "searchKnowledgeGraph cannot be null");
        this.getKnowledgeGraphStatistics = Objects.requireNonNull(getKnowledgeGraphStatistics, "getKnowledgeGraphStatistics cannot be null");
        this.dtoMapper = Objects.requireNonNull(dtoMapper, "dtoMapper cannot be null");
    }
    
    @GetMapping("/graphs")
    public ResponseEntity<ApiResponse<List<KnowledgeGraphSummaryDto>>> listGraphs() {
        try {
            BrowseKnowledgeGraphInput input = new BrowseKnowledgeGraphInput(
                BrowseType.LIST_GRAPHS,
                null, // no specific graph
                null, // no node filter
                0,    // page
                50    // size
            );
            
            BrowseKnowledgeGraphOutput output = browseKnowledgeGraph.execute(input);
            
            List<KnowledgeGraphSummaryDto> graphSummaries = output.getGraphs().stream()
                .map(dtoMapper::toSummaryDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(graphSummaries));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("GRAPH_LIST_FAILED", e.getMessage()));
        }
    }
    
    @GetMapping("/graphs/{graphId}")
    public ResponseEntity<ApiResponse<KnowledgeGraphDto>> getGraph(
        @PathVariable String graphId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size
    ) {
        try {
            BrowseKnowledgeGraphInput input = new BrowseKnowledgeGraphInput(
                BrowseType.GET_GRAPH,
                new GraphId(graphId),
                null,
                page,
                size
            );
            
            BrowseKnowledgeGraphOutput output = browseKnowledgeGraph.execute(input);
            
            if (output.getGraphs().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            KnowledgeGraphDto graphDto = dtoMapper.toDto(output.getGraphs().get(0));
            return ResponseEntity.ok(ApiResponse.success(graphDto));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.failure("INVALID_GRAPH_ID", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("GRAPH_RETRIEVAL_FAILED", e.getMessage()));
        }
    }
    
    @GetMapping("/graphs/{graphId}/nodes/{nodeId}")
    public ResponseEntity<ApiResponse<KnowledgeNodeDetailDto>> getNodeDetails(
        @PathVariable String graphId,
        @PathVariable String nodeId
    ) {
        try {
            BrowseKnowledgeGraphInput input = new BrowseKnowledgeGraphInput(
                BrowseType.GET_NODE_DETAILS,
                new GraphId(graphId),
                new NodeId(nodeId),
                0,
                1
            );
            
            BrowseKnowledgeGraphOutput output = browseKnowledgeGraph.execute(input);
            
            if (output.getNodes().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            KnowledgeNodeDetailDto nodeDto = dtoMapper.toDetailDto(
                output.getNodes().get(0),
                output.getConnectedNodes(),
                output.getRelationships()
            );
            
            return ResponseEntity.ok(ApiResponse.success(nodeDto));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.failure("INVALID_NODE_ID", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("NODE_RETRIEVAL_FAILED", e.getMessage()));
        }
    }
    
    @GetMapping("/graphs/{graphId}/subgraph")
    public ResponseEntity<ApiResponse<KnowledgeSubgraphDto>> getSubgraph(
        @PathVariable String graphId,
        @RequestParam String centerNodeId,
        @RequestParam(defaultValue = "2") int depth
    ) {
        try {
            BrowseKnowledgeGraphInput input = new BrowseKnowledgeGraphInput(
                BrowseType.GET_SUBGRAPH,
                new GraphId(graphId),
                new NodeId(centerNodeId),
                0,
                1000 // Large size for subgraph
            );
            input = input.withDepth(depth);
            
            BrowseKnowledgeGraphOutput output = browseKnowledgeGraph.execute(input);
            
            KnowledgeSubgraphDto subgraphDto = dtoMapper.toSubgraphDto(
                output.getNodes(),
                output.getRelationships(),
                centerNodeId,
                depth
            );
            
            return ResponseEntity.ok(ApiResponse.success(subgraphDto));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.failure("INVALID_SUBGRAPH_REQUEST", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("SUBGRAPH_RETRIEVAL_FAILED", e.getMessage()));
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<KnowledgeSearchResultDto>> searchGraph(
        @RequestParam String query,
        @RequestParam(required = false) String graphId,
        @RequestParam(required = false) List<String> nodeTypes,
        @RequestParam(required = false) List<String> relationshipTypes,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            SearchKnowledgeGraphInput input = new SearchKnowledgeGraphInput(
                query,
                graphId != null ? new GraphId(graphId) : null,
                nodeTypes != null ? nodeTypes.stream().map(NodeType::valueOf).collect(Collectors.toList()) : null,
                relationshipTypes != null ? relationshipTypes.stream().map(RelationshipType::valueOf).collect(Collectors.toList()) : null,
                page,
                size
            );
            
            SearchKnowledgeGraphOutput output = searchKnowledgeGraph.execute(input);
            
            KnowledgeSearchResultDto resultDto = dtoMapper.toSearchResultDto(output);
            
            return ResponseEntity.ok(ApiResponse.success(resultDto));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.failure("INVALID_SEARCH_PARAMETERS", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("SEARCH_FAILED", e.getMessage()));
        }
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<KnowledgeGraphStatisticsDto>> getStatistics(
        @RequestParam(required = false) String graphId
    ) {
        try {
            GetKnowledgeGraphStatisticsInput input = new GetKnowledgeGraphStatisticsInput(
                graphId != null ? new GraphId(graphId) : null
            );
            
            GetKnowledgeGraphStatisticsOutput output = getKnowledgeGraphStatistics.execute(input);
            
            KnowledgeGraphStatisticsDto statisticsDto = dtoMapper.toStatisticsDto(output);
            
            return ResponseEntity.ok(ApiResponse.success(statisticsDto));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("STATISTICS_RETRIEVAL_FAILED", e.getMessage()));
        }
    }
}
```

### API DTOs

**KnowledgeGraphDto:**
```java
public record KnowledgeGraphDto(
    String graphId,
    String name,
    List<KnowledgeNodeDto> nodes,
    List<KnowledgeRelationshipDto> relationships,
    KnowledgeGraphMetadataDto metadata,
    String createdAt,
    String lastUpdatedAt,
    int totalNodes,
    int totalRelationships
) {
    public static KnowledgeGraphDto fromDomain(KnowledgeGraph graph) {
        return new KnowledgeGraphDto(
            graph.getGraphId().value(),
            graph.getName(),
            graph.getNodes().stream()
                .map(KnowledgeNodeDto::fromDomain)
                .collect(Collectors.toList()),
            graph.getRelationships().stream()
                .map(KnowledgeRelationshipDto::fromDomain)
                .collect(Collectors.toList()),
            KnowledgeGraphMetadataDto.fromDomain(graph.getMetadata()),
            graph.getCreatedAt().toString(),
            graph.getLastUpdatedAt().toString(),
            graph.getNodes().size(),
            graph.getRelationships().size()
        );
    }
}
```

**KnowledgeNodeDto:**
```java
public record KnowledgeNodeDto(
    String nodeId,
    String label,
    String nodeType,
    Map<String, Object> properties,
    DocumentReferenceDto sourceDocument,
    double confidence,
    String createdAt,
    String lastUpdatedAt
) {
    public static KnowledgeNodeDto fromDomain(KnowledgeNode node) {
        return new KnowledgeNodeDto(
            node.getNodeId().value(),
            node.getLabel(),
            node.getNodeType().name(),
            node.getProperties(),
            node.getSourceDocument() != null ? 
                DocumentReferenceDto.fromDomain(node.getSourceDocument()) : null,
            node.getConfidence().value(),
            node.getCreatedAt().toString(),
            node.getLastUpdatedAt().toString()
        );
    }
}
```

**KnowledgeNodeDetailDto:**
```java
public record KnowledgeNodeDetailDto(
    KnowledgeNodeDto node,
    List<KnowledgeNodeDto> connectedNodes,
    List<KnowledgeRelationshipDto> relationships,
    int connectionCount,
    List<String> relationshipTypes
) {
    public static KnowledgeNodeDetailDto fromDomain(
        KnowledgeNode node,
        List<KnowledgeNode> connectedNodes,
        List<KnowledgeRelationship> relationships
    ) {
        return new KnowledgeNodeDetailDto(
            KnowledgeNodeDto.fromDomain(node),
            connectedNodes.stream()
                .map(KnowledgeNodeDto::fromDomain)
                .collect(Collectors.toList()),
            relationships.stream()
                .map(KnowledgeRelationshipDto::fromDomain)
                .collect(Collectors.toList()),
            connectedNodes.size(),
            relationships.stream()
                .map(rel -> rel.getRelationshipType().name())
                .distinct()
                .collect(Collectors.toList())
        );
    }
}
```

**KnowledgeSubgraphDto:**
```java
public record KnowledgeSubgraphDto(
    String centerNodeId,
    int depth,
    List<KnowledgeNodeDto> nodes,
    List<KnowledgeRelationshipDto> relationships,
    GraphVisualizationDto visualization
) {
    public static KnowledgeSubgraphDto fromDomain(
        List<KnowledgeNode> nodes,
        List<KnowledgeRelationship> relationships,
        String centerNodeId,
        int depth
    ) {
        List<KnowledgeNodeDto> nodeDtos = nodes.stream()
            .map(KnowledgeNodeDto::fromDomain)
            .collect(Collectors.toList());
        
        List<KnowledgeRelationshipDto> relationshipDtos = relationships.stream()
            .map(KnowledgeRelationshipDto::fromDomain)
            .collect(Collectors.toList());
        
        GraphVisualizationDto visualization = GraphVisualizationDto.fromNodes(nodeDtos, relationshipDtos);
        
        return new KnowledgeSubgraphDto(
            centerNodeId,
            depth,
            nodeDtos,
            relationshipDtos,
            visualization
        );
    }
}
```

**GraphVisualizationDto:**
```java
public record GraphVisualizationDto(
    List<VisualizationNodeDto> nodes,
    List<VisualizationEdgeDto> edges,
    Map<String, Integer> nodeTypeCounts,
    Map<String, Integer> relationshipTypeCounts
) {
    public static GraphVisualizationDto fromNodes(
        List<KnowledgeNodeDto> nodes,
        List<KnowledgeRelationshipDto> relationships
    ) {
        List<VisualizationNodeDto> vizNodes = nodes.stream()
            .map(node -> new VisualizationNodeDto(
                node.nodeId(),
                node.label(),
                node.nodeType(),
                node.confidence(),
                getNodeColor(node.nodeType()),
                getNodeSize(node.confidence())
            ))
            .collect(Collectors.toList());
        
        List<VisualizationEdgeDto> vizEdges = relationships.stream()
            .map(rel -> new VisualizationEdgeDto(
                rel.relationshipId(),
                rel.fromNodeId(),
                rel.toNodeId(),
                rel.relationshipType(),
                rel.confidence(),
                getEdgeColor(rel.relationshipType()),
                getEdgeWidth(rel.confidence())
            ))
            .collect(Collectors.toList());
        
        Map<String, Integer> nodeTypeCounts = nodes.stream()
            .collect(Collectors.groupingBy(
                KnowledgeNodeDto::nodeType,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        Map<String, Integer> relationshipTypeCounts = relationships.stream()
            .collect(Collectors.groupingBy(
                KnowledgeRelationshipDto::relationshipType,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        return new GraphVisualizationDto(
            vizNodes,
            vizEdges,
            nodeTypeCounts,
            relationshipTypeCounts
        );
    }
    
    private static String getNodeColor(String nodeType) {
        return switch (nodeType) {
            case "CONCEPT" -> "#4CAF50";
            case "ENTITY" -> "#2196F3";
            case "PERSON" -> "#FF9800";
            case "ORGANIZATION" -> "#9C27B0";
            case "LOCATION" -> "#F44336";
            default -> "#757575";
        };
    }
    
    private static int getNodeSize(double confidence) {
        if (confidence >= 0.8) return 20;
        if (confidence >= 0.6) return 15;
        return 10;
    }
    
    private static String getEdgeColor(String relationshipType) {
        return switch (relationshipType) {
            case "RELATED_TO" -> "#666666";
            case "PART_OF" -> "#4CAF50";
            case "MENTIONS" -> "#2196F3";
            case "REFERENCES" -> "#FF9800";
            default -> "#CCCCCC";
        };
    }
    
    private static int getEdgeWidth(double confidence) {
        if (confidence >= 0.8) return 3;
        if (confidence >= 0.6) return 2;
        return 1;
    }
}
```

### Use Cases for API

**BrowseKnowledgeGraph Use Case:**
```java
@ApplicationScoped
public final class BrowseKnowledgeGraph {
    
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    
    public BrowseKnowledgeGraph(KnowledgeGraphRepository knowledgeGraphRepository) {
        this.knowledgeGraphRepository = Objects.requireNonNull(knowledgeGraphRepository, "knowledgeGraphRepository cannot be null");
    }
    
    public BrowseKnowledgeGraphOutput execute(BrowseKnowledgeGraphInput input) {
        Objects.requireNonNull(input, "input cannot be null");
        
        return switch (input.getBrowseType()) {
            case LIST_GRAPHS -> listGraphs(input);
            case GET_GRAPH -> getGraph(input);
            case GET_NODE_DETAILS -> getNodeDetails(input);
            case GET_SUBGRAPH -> getSubgraph(input);
        };
    }
    
    private BrowseKnowledgeGraphOutput listGraphs(BrowseKnowledgeGraphInput input) {
        List<KnowledgeGraph> graphs = knowledgeGraphRepository.findAll();
        
        // Apply pagination
        int start = input.getPage() * input.getSize();
        int end = Math.min(start + input.getSize(), graphs.size());
        List<KnowledgeGraph> paginatedGraphs = graphs.subList(start, end);
        
        return BrowseKnowledgeGraphOutput.success(
            paginatedGraphs,
            List.of(),
            List.of(),
            List.of(),
            graphs.size()
        );
    }
    
    private BrowseKnowledgeGraphOutput getGraph(BrowseKnowledgeGraphInput input) {
        Optional<KnowledgeGraph> graph = knowledgeGraphRepository.findById(input.getGraphId());
        
        if (graph.isEmpty()) {
            return BrowseKnowledgeGraphOutput.notFound("Graph not found: " + input.getGraphId());
        }
        
        return BrowseKnowledgeGraphOutput.success(
            List.of(graph.get()),
            new ArrayList<>(graph.get().getNodes()),
            new ArrayList<>(graph.get().getRelationships()),
            List.of(),
            1
        );
    }
    
    private BrowseKnowledgeGraphOutput getNodeDetails(BrowseKnowledgeGraphInput input) {
        List<KnowledgeNode> connectedNodes = knowledgeGraphRepository.findNodesConnectedTo(input.getNodeId());
        
        // Find the target node and its relationships
        // Implementation depends on repository capabilities
        
        return BrowseKnowledgeGraphOutput.success(
            List.of(),
            List.of(), // target node
            List.of(), // relationships
            connectedNodes,
            1
        );
    }
    
    private BrowseKnowledgeGraphOutput getSubgraph(BrowseKnowledgeGraphInput input) {
        KnowledgeGraph subgraph = knowledgeGraphRepository.findSubgraphAroundNode(
            input.getNodeId(),
            input.getDepth()
        );
        
        return BrowseKnowledgeGraphOutput.success(
            List.of(subgraph),
            new ArrayList<>(subgraph.getNodes()),
            new ArrayList<>(subgraph.getRelationships()),
            List.of(),
            1
        );
    }
}
```

## Files / Modules Impacted

- `backend/application-integration/src/main/java/com/rag/app/integration/api/controllers/KnowledgeGraphController.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeGraphDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeNodeDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeRelationshipDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeSubgraphDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/GraphVisualizationDto.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/BrowseKnowledgeGraph.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/SearchKnowledgeGraph.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/GetKnowledgeGraphStatistics.java`

## Expected Behavior

**Given** an admin user requests the list of knowledge graphs  
**When** the API is called  
**Then** all available graphs should be returned with summary information

**Given** an admin user requests a specific graph  
**When** the graph ID is provided  
**Then** the complete graph with nodes and relationships should be returned

**Given** an admin user searches for knowledge  
**When** search parameters are provided  
**Then** matching nodes and relationships should be returned with relevance scoring

## Acceptance Criteria

- ✅ All endpoints require admin authorization
- ✅ API returns proper HTTP status codes and error messages
- ✅ Graph data is properly formatted for frontend consumption
- ✅ Pagination is supported for large graphs
- ✅ Search functionality works across nodes and relationships
- ✅ Visualization data includes proper styling information
- ✅ Statistics provide useful insights about graph structure

## Testing Requirements

- Unit tests for all controller methods
- Integration tests with real knowledge graph data
- Tests for authorization and access control
- Tests for pagination and large datasets
- Tests for search functionality
- Performance tests for graph visualization endpoints

## Dependencies / Preconditions

- Knowledge graph domain entities and use cases
- Neo4j persistence implementation
- Admin user authorization system
- Existing API infrastructure

## Implementation Notes

### API Design Principles

- Follow REST conventions for resource naming
- Use consistent error response format
- Support pagination for all list endpoints
- Include metadata in responses for frontend use

### Visualization Considerations

- Provide node positioning hints for graph layout
- Include styling information (colors, sizes) based on node types
- Support different visualization modes (force-directed, hierarchical)
- Limit subgraph size to prevent performance issues

### Security Considerations

- Ensure only admin users can access graph administration
- Validate all input parameters to prevent injection attacks
- Rate limit search endpoints to prevent abuse
- Log all administrative actions for audit purposes
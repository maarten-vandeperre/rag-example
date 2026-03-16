package com.rag.app.shared.usecases.knowledge;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.valueobjects.ConfidenceScore;
import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphMetadata;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeType;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import com.rag.app.shared.usecases.knowledge.models.BrowseKnowledgeGraphInput;
import com.rag.app.shared.usecases.knowledge.models.BrowseType;
import com.rag.app.shared.usecases.knowledge.models.GetKnowledgeGraphStatisticsInput;
import com.rag.app.shared.usecases.knowledge.models.SearchKnowledgeGraphInput;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeGraphAdministrationUseCasesTest {
    private static final Instant NOW = Instant.parse("2026-03-16T16:00:00Z");

    @Test
    void shouldBrowseGraphsAndNodeDetails() {
        InMemoryKnowledgeGraphRepository repository = new InMemoryKnowledgeGraphRepository(List.of(sampleGraph()));
        BrowseKnowledgeGraph useCase = new BrowseKnowledgeGraph(repository);

        var listOutput = useCase.execute(new BrowseKnowledgeGraphInput(BrowseType.LIST_GRAPHS, null, null, 0, 10));
        var graphOutput = useCase.execute(new BrowseKnowledgeGraphInput(BrowseType.GET_GRAPH, new GraphId("graph-admin"), null, 0, 10));
        var nodeOutput = useCase.execute(new BrowseKnowledgeGraphInput(BrowseType.GET_NODE_DETAILS, new GraphId("graph-admin"), new NodeId("node-1"), 0, 10));
        var subgraphOutput = useCase.execute(new BrowseKnowledgeGraphInput(BrowseType.GET_SUBGRAPH, new GraphId("graph-admin"), new NodeId("node-1"), 0, 10).withDepth(2));

        assertEquals(1, listOutput.graphs().size());
        assertEquals(2, graphOutput.nodes().size());
        assertEquals(1, nodeOutput.connectedNodes().size());
        assertEquals(1, nodeOutput.relationships().size());
        assertEquals(2, subgraphOutput.nodes().size());
    }

    @Test
    void shouldSearchAndCalculateStatistics() {
        InMemoryKnowledgeGraphRepository repository = new InMemoryKnowledgeGraphRepository(List.of(sampleGraph()));
        SearchKnowledgeGraph searchUseCase = new SearchKnowledgeGraph(repository);
        GetKnowledgeGraphStatistics statisticsUseCase = new GetKnowledgeGraphStatistics(repository);

        var searchOutput = searchUseCase.execute(new SearchKnowledgeGraphInput("knowledge", null, List.of(NodeType.TOPIC), List.of(RelationshipType.RELATED_TO), 0, 10));
        var statisticsOutput = statisticsUseCase.execute(new GetKnowledgeGraphStatisticsInput(null));

        assertEquals(1, searchOutput.nodes().size());
        assertEquals(1, searchOutput.relationships().size());
        assertEquals(1, searchOutput.graphs().size());
        assertEquals(1, statisticsOutput.totalGraphs());
        assertEquals(2, statisticsOutput.totalNodes());
        assertEquals(1, statisticsOutput.totalRelationships());
        assertTrue(statisticsOutput.nodeTypeCounts().containsKey("TOPIC"));
        assertTrue(statisticsOutput.averageNodeConfidence() > 0.0d);
    }

    @Test
    void shouldReturnNotFoundWhenGraphOrNodeMissing() {
        InMemoryKnowledgeGraphRepository repository = new InMemoryKnowledgeGraphRepository(List.of(sampleGraph()));
        BrowseKnowledgeGraph useCase = new BrowseKnowledgeGraph(repository);

        var missingGraph = useCase.execute(new BrowseKnowledgeGraphInput(BrowseType.GET_GRAPH, new GraphId("missing"), null, 0, 10));
        var missingNode = useCase.execute(new BrowseKnowledgeGraphInput(BrowseType.GET_NODE_DETAILS, new GraphId("graph-admin"), new NodeId("missing-node"), 0, 10));

        assertFalse(missingGraph.found());
        assertFalse(missingNode.found());
    }

    private static KnowledgeGraph sampleGraph() {
        DocumentReference sourceDocument = new DocumentReference(UUID.randomUUID(), "knowledge-admin.md", "intro", 0.91d);
        KnowledgeNode topic = new KnowledgeNode(new NodeId("node-1"), "Knowledge Graph", NodeType.TOPIC, Map.of("category", "admin"), sourceDocument, ConfidenceScore.high(), NOW, NOW);
        KnowledgeNode entity = new KnowledgeNode(new NodeId("node-2"), "Neo4j", NodeType.ENTITY, Map.of("vendor", "neo4j"), sourceDocument, new ConfidenceScore(0.8d), NOW.plusSeconds(1), NOW.plusSeconds(1));
        KnowledgeRelationship relationship = KnowledgeRelationship.create(topic.nodeId(), entity.nodeId(), RelationshipType.RELATED_TO, Map.of("evidence", "section-2"), sourceDocument, new ConfidenceScore(0.7d), NOW.plusSeconds(2));
        return new KnowledgeGraph(new GraphId("graph-admin"), "Administration Graph", Set.of(topic, entity), Set.of(relationship), new GraphMetadata("Administration graph", Set.of(sourceDocument), Map.of("owner", "admin")), NOW, NOW.plusSeconds(3));
    }

    private static final class InMemoryKnowledgeGraphRepository implements KnowledgeGraphRepository {
        private final Map<GraphId, KnowledgeGraph> graphs = new LinkedHashMap<>();

        private InMemoryKnowledgeGraphRepository(List<KnowledgeGraph> graphs) {
            graphs.forEach(graph -> this.graphs.put(graph.graphId(), graph));
        }

        @Override
        public KnowledgeGraph save(KnowledgeGraph knowledgeGraph) {
            graphs.put(knowledgeGraph.graphId(), knowledgeGraph);
            return knowledgeGraph;
        }

        @Override
        public Optional<KnowledgeGraph> findById(GraphId graphId) {
            return Optional.ofNullable(graphs.get(graphId));
        }

        @Override
        public Optional<KnowledgeGraph> findByName(String name) {
            return graphs.values().stream().filter(graph -> graph.name().equals(name)).findFirst();
        }

        @Override
        public List<KnowledgeGraph> findAll() {
            return graphs.values().stream().toList();
        }

        @Override
        public void delete(GraphId graphId) {
            graphs.remove(graphId);
        }

        @Override
        public boolean existsByName(String name) {
            return findByName(name).isPresent();
        }

        @Override
        public List<KnowledgeNode> findNodesConnectedTo(NodeId nodeId) {
            return graphs.values().stream()
                .filter(graph -> graph.containsNode(nodeId))
                .flatMap(graph -> graph.relationshipsFor(nodeId).stream())
                .flatMap(relationship -> List.of(relationship.fromNodeId(), relationship.toNodeId()).stream())
                .filter(connectedNodeId -> !connectedNodeId.equals(nodeId))
                .map(connectedNodeId -> graphs.values().stream().map(graph -> graph.getNode(connectedNodeId)).flatMap(Optional::stream).findFirst().orElseThrow())
                .distinct()
                .toList();
        }

        @Override
        public List<KnowledgeRelationship> findRelationshipsByType(RelationshipType type) {
            return graphs.values().stream().flatMap(graph -> graph.relationships().stream()).filter(relationship -> relationship.relationshipType() == type).toList();
        }

        @Override
        public KnowledgeGraph findSubgraphAroundNode(NodeId nodeId, int depth) {
            return graphs.values().stream().filter(graph -> graph.containsNode(nodeId)).findFirst().orElseThrow();
        }
    }
}

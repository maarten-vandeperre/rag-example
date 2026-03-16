package com.rag.app.integration;

import com.rag.app.integration.api.controllers.KnowledgeGraphController;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphDtoMapper;
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
import com.rag.app.shared.usecases.knowledge.BrowseKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.GetKnowledgeGraphStatistics;
import com.rag.app.shared.usecases.knowledge.SearchKnowledgeGraph;
import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;
import com.rag.app.user.interfaces.UserManagementFacade;
import com.rag.app.user.usecases.models.AuthenticationRequest;
import com.rag.app.user.usecases.models.AuthenticationResult;
import com.rag.app.user.usecases.models.GetUserProfileInput;
import com.rag.app.user.usecases.models.GetUserProfileOutput;
import com.rag.app.user.usecases.models.ManageUserRolesInput;
import com.rag.app.user.usecases.models.ManageUserRolesOutput;
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

class KnowledgeGraphControllerTest {
    private static final String ADMIN_USER_ID = "33333333-3333-3333-3333-333333333333";
    private static final String STANDARD_USER_ID = "44444444-4444-4444-4444-444444444444";
    private static final Instant NOW = Instant.parse("2026-03-16T17:00:00Z");

    @Test
    void shouldExposeKnowledgeGraphAdministrationResponses() {
        InMemoryKnowledgeGraphRepository repository = new InMemoryKnowledgeGraphRepository(List.of(sampleGraph()));
        KnowledgeGraphController controller = controller(repository, UserRole.ADMIN);

        var listResponse = controller.listGraphs(ADMIN_USER_ID, 0, 20);
        var graphResponse = controller.getGraph(ADMIN_USER_ID, "graph-api", 0, 20);
        var nodeResponse = controller.getNodeDetails(ADMIN_USER_ID, "graph-api", "node-1");
        var subgraphResponse = controller.getSubgraph(ADMIN_USER_ID, "graph-api", "node-1", 2);
        var searchResponse = controller.searchGraph(ADMIN_USER_ID, "knowledge", null, List.of("TOPIC"), List.of("RELATED_TO"), 0, 20);
        var statisticsResponse = controller.getStatistics(ADMIN_USER_ID, null);

        assertTrue(listResponse.success());
        assertEquals(1, listResponse.data().size());
        assertTrue(graphResponse.success());
        assertEquals(2, graphResponse.data().totalNodes());
        assertTrue(nodeResponse.success());
        assertEquals(1, nodeResponse.data().connectionCount());
        assertTrue(subgraphResponse.success());
        assertEquals("node-1", subgraphResponse.data().centerNodeId());
        assertTrue(searchResponse.success());
        assertEquals(1, searchResponse.data().nodes().size());
        assertTrue(statisticsResponse.success());
        assertEquals(1, statisticsResponse.data().totalGraphs());
    }

    @Test
    void shouldRejectNonAdminRequestsAndInvalidInputs() {
        InMemoryKnowledgeGraphRepository repository = new InMemoryKnowledgeGraphRepository(List.of(sampleGraph()));
        KnowledgeGraphController nonAdminController = controller(repository, UserRole.STANDARD);
        KnowledgeGraphController adminController = controller(repository, UserRole.ADMIN);

        var unauthorizedResponse = nonAdminController.listGraphs(STANDARD_USER_ID, 0, 10);
        var invalidResponse = adminController.searchGraph(ADMIN_USER_ID, "", null, List.of(), List.of(), 0, 10);
        var missingGraphResponse = adminController.getGraph(ADMIN_USER_ID, "missing", 0, 10);

        assertFalse(unauthorizedResponse.success());
        assertEquals("ADMIN_ACCESS_REQUIRED", unauthorizedResponse.error().code());
        assertFalse(invalidResponse.success());
        assertEquals("INVALID_SEARCH_PARAMETERS", invalidResponse.error().code());
        assertFalse(missingGraphResponse.success());
        assertEquals("GRAPH_NOT_FOUND", missingGraphResponse.error().code());
    }

    private KnowledgeGraphController controller(InMemoryKnowledgeGraphRepository repository, UserRole role) {
        return new KnowledgeGraphController(
            new BrowseKnowledgeGraph(repository),
            new SearchKnowledgeGraph(repository),
            new GetKnowledgeGraphStatistics(repository),
            new KnowledgeGraphDtoMapper(),
            new StubUserManagementFacade(role)
        );
    }

    private static KnowledgeGraph sampleGraph() {
        DocumentReference sourceDocument = new DocumentReference(UUID.randomUUID(), "graph-api.md", "summary", 0.93d);
        KnowledgeNode topic = new KnowledgeNode(new NodeId("node-1"), "Knowledge Graph", NodeType.TOPIC, Map.of("category", "api"), sourceDocument, ConfidenceScore.high(), NOW, NOW);
        KnowledgeNode entity = new KnowledgeNode(new NodeId("node-2"), "Neo4j", NodeType.ENTITY, Map.of("vendor", "neo4j"), sourceDocument, new ConfidenceScore(0.82d), NOW.plusSeconds(1), NOW.plusSeconds(1));
        KnowledgeRelationship relationship = KnowledgeRelationship.create(topic.nodeId(), entity.nodeId(), RelationshipType.RELATED_TO, Map.of("evidence", "summary"), sourceDocument, new ConfidenceScore(0.76d), NOW.plusSeconds(2));
        return new KnowledgeGraph(new GraphId("graph-api"), "API Graph", Set.of(topic, entity), Set.of(relationship), new GraphMetadata("Knowledge API graph", Set.of(sourceDocument), Map.of("owner", "integration")), NOW, NOW.plusSeconds(3));
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
                .map(this::findNode)
                .flatMap(Optional::stream)
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

        private Optional<KnowledgeNode> findNode(NodeId nodeId) {
            return graphs.values().stream().map(graph -> graph.getNode(nodeId)).flatMap(Optional::stream).findFirst();
        }
    }

    private static final class StubUserManagementFacade implements UserManagementFacade {
        private final User user;

        private StubUserManagementFacade(UserRole role) {
            this.user = new User(new UserId(UUID.fromString(role == UserRole.ADMIN ? ADMIN_USER_ID : STANDARD_USER_ID)), role.roleName(), role.roleName() + "@example.com", role, NOW, true);
        }

        @Override
        public AuthenticationResult authenticateUser(AuthenticationRequest request) {
            return new AuthenticationResult(true, "session", user, null);
        }

        @Override
        public void invalidateSession(String sessionToken) {
        }

        @Override
        public boolean isAuthorized(UserId userId, String resource, String action) {
            return user.role() == UserRole.ADMIN;
        }

        @Override
        public UserRole getUserRole(UserId userId) {
            return user.role();
        }

        @Override
        public GetUserProfileOutput getUserProfile(GetUserProfileInput input) {
            return null;
        }

        @Override
        public Optional<User> findUserById(UserId userId) {
            return Optional.of(user);
        }

        @Override
        public boolean isActiveUser(UserId userId) {
            return true;
        }

        @Override
        public ManageUserRolesOutput manageUserRoles(ManageUserRolesInput input) {
            return null;
        }

        @Override
        public List<User> getAllUsers() {
            return List.of(user);
        }
    }
}

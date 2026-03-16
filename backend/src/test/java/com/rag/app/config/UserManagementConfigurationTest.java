package com.rag.app.config;

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
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

class UserManagementConfigurationTest {
    private static final Instant NOW = Instant.parse("2026-03-16T18:00:00Z");

    @Test
    void shouldWireRealUserManagementFacadeAndEnforceKnowledgeGraphAdminAccess() throws SQLException {
        UserManagementConfiguration configuration = new UserManagementConfiguration();
        DataSource dataSource = initializedDataSource();
        UserManagementFacade facade = configuration.userManagementFacade(
            configuration.authenticateUser(
                configuration.authenticationProvider(configuration.userRepository(dataSource)),
                configuration.sessionManager(),
                configuration.userDomainService()
            ),
            configuration.authorizeUserAction(configuration.userRepository(dataSource), configuration.authorizationService()),
            configuration.getUserProfile(configuration.userRepository(dataSource)),
            configuration.manageUserRoles(configuration.userRepository(dataSource), configuration.userDomainService()),
            configuration.userRepository(dataSource),
            configuration.sessionManager()
        );

        User admin = new User(new UserId(UUID.fromString("55555555-5555-5555-5555-555555555555")), "admin", "admin@example.com", UserRole.ADMIN, NOW, true);
        User standard = new User(new UserId(UUID.fromString("66666666-6666-6666-6666-666666666666")), "user", "user@example.com", UserRole.STANDARD, NOW.plusSeconds(1), true);
        facade.getAllUsers();
        configuration.userRepository(dataSource).save(admin);
        configuration.userRepository(dataSource).save(standard);

        KnowledgeGraphController controller = new KnowledgeGraphController(
            new BrowseKnowledgeGraph(new InMemoryKnowledgeGraphRepository(List.of(sampleGraph()))),
            new SearchKnowledgeGraph(new InMemoryKnowledgeGraphRepository(List.of(sampleGraph()))),
            new GetKnowledgeGraphStatistics(new InMemoryKnowledgeGraphRepository(List.of(sampleGraph()))),
            new KnowledgeGraphDtoMapper(),
            facade
        );

        var adminResponse = controller.listGraphs(admin.userId().toString(), 0, 10);
        var standardResponse = controller.listGraphs(standard.userId().toString(), 0, 10);

        assertTrue(adminResponse.success());
        assertEquals(1, adminResponse.data().size());
        assertFalse(standardResponse.success());
        assertEquals("ADMIN_ACCESS_REQUIRED", standardResponse.error().code());
        assertTrue(facade.isAuthorized(admin.userId(), standard.userId().toString(), "manage_users"));
        assertFalse(facade.isAuthorized(standard.userId(), admin.userId().toString(), "manage_users"));
    }

    private static DataSource initializedDataSource() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:backend-user-config-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(loadSchemaSql());
        }

        return dataSource;
    }

    private static String loadSchemaSql() {
        try (InputStream inputStream = UserManagementConfigurationTest.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (inputStream == null) {
                throw new IllegalStateException("schema.sql resource not found");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read schema.sql", exception);
        }
    }

    private static KnowledgeGraph sampleGraph() {
        DocumentReference sourceDocument = new DocumentReference(UUID.randomUUID(), "graph-config.md", "root", 0.8d);
        KnowledgeNode topic = new KnowledgeNode(new NodeId("config-node-1"), "Knowledge Graph", NodeType.TOPIC, Map.of(), sourceDocument, ConfidenceScore.high(), NOW, NOW);
        KnowledgeNode entity = new KnowledgeNode(new NodeId("config-node-2"), "Users", NodeType.ENTITY, Map.of(), sourceDocument, ConfidenceScore.medium(), NOW.plusSeconds(1), NOW.plusSeconds(1));
        KnowledgeRelationship relationship = KnowledgeRelationship.create(topic.nodeId(), entity.nodeId(), RelationshipType.RELATED_TO, Map.of(), sourceDocument, ConfidenceScore.medium(), NOW.plusSeconds(2));
        return new KnowledgeGraph(new GraphId("config-graph"), "Config Graph", Set.of(topic, entity), Set.of(relationship), new GraphMetadata("Config graph", Set.of(sourceDocument), Map.of()), NOW, NOW.plusSeconds(3));
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
}

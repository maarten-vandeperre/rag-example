package com.rag.app.shared.infrastructure.knowledge;

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
import com.rag.app.shared.infrastructure.knowledge.mappers.KnowledgeGraphMapper;
import com.rag.app.shared.infrastructure.knowledge.mappers.KnowledgeNodeMapper;
import com.rag.app.shared.infrastructure.knowledge.mappers.KnowledgeRelationshipMapper;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Neo4jKnowledgeGraphRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-03-16T15:00:00Z");
    private static final DocumentReference SOURCE_DOCUMENT = new DocumentReference(
        UUID.fromString("00000000-0000-0000-0000-000000000070"),
        "knowledge.md",
        "section-a",
        0.87d
    );

    @Test
    void shouldSaveAndLoadKnowledgeGraphThroughNeo4jRepository() {
        StubNeo4j stubNeo4j = new StubNeo4j();
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findGraphById(), result(record(map(
            entry("g", node(map(
                entry("graphId", "graph-1"),
                entry("name", "knowledge"),
                entry("description", "stored graph"),
                entry("createdAt", NOW.toString()),
                entry("lastUpdatedAt", NOW.plusSeconds(30).toString()),
                entry("sourceDocuments", List.of(SOURCE_DOCUMENT.documentId() + "|knowledge.md|section-a|0.87")),
                entry("metadataAttribute_owner", "upload")
            )))
        ))));
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findNodesByGraphId(), result(
            record(map(entry("n", node(map(
                entry("nodeId", "node-1"),
                entry("label", "Knowledge Graph"),
                entry("nodeType", "TOPIC"),
                entry("graphId", "graph-1"),
                entry("confidence", 0.9d),
                entry("createdAt", NOW.toString()),
                entry("lastUpdatedAt", NOW.toString()),
                entry("sourceDocumentId", SOURCE_DOCUMENT.documentId().toString()),
                entry("sourceDocumentName", SOURCE_DOCUMENT.documentName()),
                entry("sourceSectionReference", SOURCE_DOCUMENT.sectionReference()),
                entry("sourceRelevanceScore", SOURCE_DOCUMENT.relevanceScore()),
                entry("aliases", List.of("KG"))
            ))))),
            record(map(entry("n", node(map(
                entry("nodeId", "node-2"),
                entry("label", "Neo4j"),
                entry("nodeType", "ENTITY"),
                entry("graphId", "graph-1"),
                entry("confidence", 0.8d),
                entry("createdAt", NOW.plusSeconds(1).toString()),
                entry("lastUpdatedAt", NOW.plusSeconds(1).toString()),
                entry("sourceDocumentId", SOURCE_DOCUMENT.documentId().toString()),
                entry("sourceDocumentName", SOURCE_DOCUMENT.documentName()),
                entry("sourceSectionReference", SOURCE_DOCUMENT.sectionReference()),
                entry("sourceRelevanceScore", SOURCE_DOCUMENT.relevanceScore())
            )))))
        ));
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findRelationshipsByGraphId(), result(record(map(
            entry("r", relationship(map(
                entry("relationshipId", "rel-1"),
                entry("relationshipType", "RELATED_TO"),
                entry("confidence", 0.7d),
                entry("createdAt", NOW.plusSeconds(2).toString()),
                entry("sourceDocumentId", SOURCE_DOCUMENT.documentId().toString()),
                entry("sourceDocumentName", SOURCE_DOCUMENT.documentName()),
                entry("sourceSectionReference", SOURCE_DOCUMENT.sectionReference()),
                entry("sourceRelevanceScore", SOURCE_DOCUMENT.relevanceScore()),
                entry("evidence", "paragraph-2")
            ))),
            entry("fromNodeId", value("node-1")),
            entry("toNodeId", value("node-2"))
        ))));

        Neo4jKnowledgeGraphRepository repository = repository(stubNeo4j.driver());
        KnowledgeGraph graph = sampleGraph();

        KnowledgeGraph saved = repository.save(graph);
        Optional<KnowledgeGraph> loaded = repository.findById(graph.graphId());

        assertEquals(graph, saved);
        assertTrue(loaded.isPresent());
        assertEquals("knowledge", loaded.orElseThrow().name());
        assertEquals(2, loaded.orElseThrow().nodes().size());
        assertEquals(1, loaded.orElseThrow().relationships().size());
        assertTrue(stubNeo4j.writeQueries.contains(KnowledgeGraphCypherQueries.mergeGraph()));
    }

    @Test
    void shouldSupportLookupQueriesAndDeleteOperations() {
        StubNeo4j stubNeo4j = new StubNeo4j();
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findGraphIdByName(), result(record(map(entry("graphId", value("graph-lookup"))))));
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findGraphById(), result(record(map(
            entry("g", node(map(
                entry("graphId", "graph-lookup"),
                entry("name", "lookup"),
                entry("description", "lookup graph"),
                entry("createdAt", NOW.toString()),
                entry("lastUpdatedAt", NOW.toString()),
                entry("sourceDocuments", List.of()),
                entry("metadataAttribute_mode", "lookup")
            )))
        ))));
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findNodesByGraphId(), result());
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findRelationshipsByGraphId(), result());
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.graphExistsByName(), result(record(map(entry("total", value(1))))));
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findConnectedNodes(), result(record(map(entry("connected", node(map(
            entry("nodeId", "node-connected"),
            entry("label", "Connected"),
            entry("nodeType", "ENTITY"),
            entry("graphId", "graph-lookup"),
            entry("confidence", 0.6d),
            entry("createdAt", NOW.toString()),
            entry("lastUpdatedAt", NOW.toString()),
            entry("sourceDocumentId", SOURCE_DOCUMENT.documentId().toString()),
            entry("sourceDocumentName", SOURCE_DOCUMENT.documentName()),
            entry("sourceSectionReference", SOURCE_DOCUMENT.sectionReference()),
            entry("sourceRelevanceScore", SOURCE_DOCUMENT.relevanceScore())
        )))))));
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findRelationshipsByType(), result(record(map(
            entry("r", relationship(map(
                entry("relationshipId", "rel-type"),
                entry("relationshipType", "RELATED_TO"),
                entry("confidence", 0.5d),
                entry("createdAt", NOW.toString()),
                entry("sourceDocumentId", SOURCE_DOCUMENT.documentId().toString()),
                entry("sourceDocumentName", SOURCE_DOCUMENT.documentName()),
                entry("sourceSectionReference", SOURCE_DOCUMENT.sectionReference()),
                entry("sourceRelevanceScore", SOURCE_DOCUMENT.relevanceScore())
            ))),
            entry("fromNodeId", value("node-a")),
            entry("toNodeId", value("node-b"))
        ))));
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findAllGraphIds(), result(record(map(entry("graphId", value("graph-lookup"))))));
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findGraphById(), result(record(map(
            entry("g", node(map(
                entry("graphId", "graph-lookup"),
                entry("name", "lookup"),
                entry("description", "lookup graph"),
                entry("createdAt", NOW.toString()),
                entry("lastUpdatedAt", NOW.toString()),
                entry("sourceDocuments", List.of())
            )))
        ))));
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findNodesByGraphId(), result());
        stubNeo4j.addReadResult(KnowledgeGraphCypherQueries.findRelationshipsByGraphId(), result());

        Neo4jKnowledgeGraphRepository repository = repository(stubNeo4j.driver());

        assertTrue(repository.findByName("lookup").isPresent());
        assertTrue(repository.existsByName("lookup"));
        assertEquals(1, repository.findNodesConnectedTo(new NodeId("node-root")).size());
        assertEquals(1, repository.findRelationshipsByType(RelationshipType.RELATED_TO).size());
        assertEquals(1, repository.findAll().size());

        repository.delete(new GraphId("graph-lookup"));

        assertTrue(stubNeo4j.writeQueries.contains(KnowledgeGraphCypherQueries.deleteGraph()));
    }

    @Test
    void shouldMapNeo4jEntitiesAndCreateConfiguration() {
        KnowledgeNodeMapper nodeMapper = new KnowledgeNodeMapper();
        KnowledgeRelationshipMapper relationshipMapper = new KnowledgeRelationshipMapper();
        KnowledgeGraphMapper graphMapper = new KnowledgeGraphMapper();

        KnowledgeNode node = sampleGraph().nodes().stream().findFirst().orElseThrow();
        Map<String, Object> nodeProperties = nodeMapper.toNeo4jProperties(node, sampleGraph().graphId());
        KnowledgeNode mappedNode = nodeMapper.fromNeo4jNode(node(nodeProperties));

        KnowledgeRelationship relationship = sampleGraph().relationships().stream().findFirst().orElseThrow();
        Map<String, Object> relationshipProperties = relationshipMapper.toNeo4jProperties(relationship);
        KnowledgeRelationship mappedRelationship = relationshipMapper.fromNeo4jRelationship(relationship(relationshipProperties), relationship.fromNodeId().value(), relationship.toNodeId().value());

        Record graphRecord = record(map(entry("g", node(graphMapper.toNeo4jProperties(sampleGraph())))));
        KnowledgeGraph mappedGraph = graphMapper.fromNeo4jRecord(graphRecord, sampleGraph().nodes(), sampleGraph().relationships());

        try (Neo4jConfiguration configuration = Neo4jConfiguration.defaults()) {
            assertEquals(node.label(), mappedNode.label());
            assertEquals(relationship.relationshipType(), mappedRelationship.relationshipType());
            assertEquals(sampleGraph().metadata().description(), mappedGraph.metadata().description());
            assertEquals("neo4j", configuration.database());
            assertNotNull(configuration.sessionConfig());
        }
    }

    private Neo4jKnowledgeGraphRepository repository(Driver driver) {
        return new Neo4jKnowledgeGraphRepository(
            driver,
            Neo4jConfiguration.defaults(),
            new KnowledgeGraphMapper(),
            new KnowledgeNodeMapper(),
            new KnowledgeRelationshipMapper()
        );
    }

    private KnowledgeGraph sampleGraph() {
        KnowledgeNode topic = new KnowledgeNode(
            new NodeId("node-1"),
            "Knowledge Graph",
            NodeType.TOPIC,
            Map.of("aliases", List.of("KG")),
            SOURCE_DOCUMENT,
            ConfidenceScore.high(),
            NOW,
            NOW
        );
        KnowledgeNode storage = new KnowledgeNode(
            new NodeId("node-2"),
            "Neo4j",
            NodeType.ENTITY,
            Map.of(),
            SOURCE_DOCUMENT,
            new ConfidenceScore(0.8d),
            NOW.plusSeconds(1),
            NOW.plusSeconds(1)
        );
        KnowledgeRelationship relationship = KnowledgeRelationship.create(
            topic.nodeId(),
            storage.nodeId(),
            RelationshipType.RELATED_TO,
            Map.of("evidence", "paragraph-2"),
            SOURCE_DOCUMENT,
            new ConfidenceScore(0.7d),
            NOW.plusSeconds(2)
        );
        return new KnowledgeGraph(
            new GraphId("graph-1"),
            "knowledge",
            java.util.Set.of(topic, storage),
            java.util.Set.of(relationship),
            new GraphMetadata("stored graph", java.util.Set.of(SOURCE_DOCUMENT), Map.of("owner", "upload")),
            NOW,
            NOW.plusSeconds(30)
        );
    }

    private static Result result(Record... records) {
        List<Record> values = List.of(records);
        InvocationHandler handler = new InvocationHandler() {
            private int index = 0;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "hasNext" -> index < values.size();
                    case "next" -> values.get(index++);
                    case "single" -> values.get(0);
                    case "list" -> values;
                    case "consume" -> null;
                    default -> defaultValue(method.getReturnType());
                };
            }
        };
        return (Result) Proxy.newProxyInstance(Result.class.getClassLoader(), new Class<?>[]{Result.class}, handler);
    }

    @SafeVarargs
    private static <K, V> Map<K, V> map(Map.Entry<K, V>... entries) {
        Map<K, V> values = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : entries) {
            values.put(entry.getKey(), entry.getValue());
        }
        return values;
    }

    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return Map.entry(key, value);
    }

    private static Record record(Map<String, Object> values) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "get" -> {
                Object key = args[0];
                if (key instanceof String name) {
                    yield values.get(name) instanceof Value value ? value : value(values.get(name));
                }
                yield defaultValue(method.getReturnType());
            }
            case "keys" -> List.copyOf(values.keySet());
            case "values" -> values.values().stream().map(entry -> entry instanceof Value value ? value : value(entry)).toList();
            default -> defaultValue(method.getReturnType());
        };
        return (Record) Proxy.newProxyInstance(Record.class.getClassLoader(), new Class<?>[]{Record.class}, handler);
    }

    private static Value value(Object rawValue) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "asNode" -> rawValue;
            case "asRelationship" -> rawValue;
            case "asString" -> String.valueOf(rawValue);
            case "asInt" -> rawValue instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(rawValue));
            case "asDouble" -> rawValue instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(rawValue));
            case "asObject" -> rawValue;
            case "asList" -> rawValue;
            case "isNull" -> rawValue == null;
            default -> defaultValue(method.getReturnType());
        };
        return (Value) Proxy.newProxyInstance(Value.class.getClassLoader(), new Class<?>[]{Value.class}, handler);
    }

    private static Node node(Map<String, Object> properties) {
        return entityProxy(Node.class, properties);
    }

    private static Relationship relationship(Map<String, Object> properties) {
        return entityProxy(Relationship.class, properties);
    }

    @SuppressWarnings("unchecked")
    private static <T> T entityProxy(Class<T> type, Map<String, Object> properties) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "keys" -> List.copyOf(properties.keySet());
            case "get" -> value(properties.get(args[0]));
            case "asMap" -> {
                Map<String, Object> result = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    result.put(entry.getKey(), entry.getValue());
                }
                yield result;
            }
            default -> defaultValue(method.getReturnType());
        };
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }

    private static final class StubNeo4j {
        private final Deque<Result> readResults = new ArrayDeque<>();
        private final List<String> writeQueries = new ArrayList<>();

        private void addReadResult(String expectedQuery, Result result) {
            readResults.addLast(result);
        }

        private Driver driver() {
            Session session = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(), new Class<?>[]{Session.class}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "executeWrite" -> invokeCallback(args[0], transaction(true));
                    case "executeRead" -> invokeCallback(args[0], transaction(false));
                    case "close" -> null;
                    default -> defaultValue(method.getReturnType());
                };
            });
            return (Driver) Proxy.newProxyInstance(Driver.class.getClassLoader(), new Class<?>[]{Driver.class}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "session" -> session;
                    case "close" -> null;
                    default -> defaultValue(method.getReturnType());
                };
            });
        }

        private TransactionContext transaction(boolean write) {
            return (TransactionContext) Proxy.newProxyInstance(TransactionContext.class.getClassLoader(), new Class<?>[]{TransactionContext.class}, (proxy, method, args) -> {
                if ("run".equals(method.getName())) {
                    String query = (String) args[0];
                    if (write) {
                        writeQueries.add(query);
                        return result();
                    }
                    return readResults.removeFirst();
                }
                return defaultValue(method.getReturnType());
            });
        }

        private Object invokeCallback(Object callback, TransactionContext transactionContext) throws Throwable {
            for (Method callbackMethod : callback.getClass().getMethods()) {
                if (callbackMethod.getDeclaringClass() != Object.class && callbackMethod.getParameterCount() == 1) {
                    return callbackMethod.invoke(callback, transactionContext);
                }
            }
            throw new IllegalStateException("Unsupported Neo4j callback type: " + callback.getClass().getName());
        }
    }
}

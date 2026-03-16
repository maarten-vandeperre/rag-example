package com.rag.app.shared.infrastructure.knowledge;

import com.rag.app.shared.domain.exceptions.RepositoryException;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;
import com.rag.app.shared.infrastructure.knowledge.mappers.KnowledgeGraphMapper;
import com.rag.app.shared.infrastructure.knowledge.mappers.KnowledgeNodeMapper;
import com.rag.app.shared.infrastructure.knowledge.mappers.KnowledgeRelationshipMapper;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Neo4jKnowledgeGraphRepository implements KnowledgeGraphRepository {
    private final Driver neo4jDriver;
    private final SessionConfig sessionConfig;
    private final KnowledgeGraphMapper knowledgeGraphMapper;
    private final KnowledgeNodeMapper nodeMapper;
    private final KnowledgeRelationshipMapper relationshipMapper;

    public Neo4jKnowledgeGraphRepository(Driver neo4jDriver,
                                         Neo4jConfiguration configuration,
                                         KnowledgeGraphMapper knowledgeGraphMapper,
                                         KnowledgeNodeMapper nodeMapper,
                                         KnowledgeRelationshipMapper relationshipMapper) {
        this.neo4jDriver = Objects.requireNonNull(neo4jDriver, "neo4jDriver cannot be null");
        this.sessionConfig = Objects.requireNonNull(configuration, "configuration cannot be null").sessionConfig();
        this.knowledgeGraphMapper = Objects.requireNonNull(knowledgeGraphMapper, "knowledgeGraphMapper cannot be null");
        this.nodeMapper = Objects.requireNonNull(nodeMapper, "nodeMapper cannot be null");
        this.relationshipMapper = Objects.requireNonNull(relationshipMapper, "relationshipMapper cannot be null");
    }

    @Override
    public KnowledgeGraph save(KnowledgeGraph knowledgeGraph) {
        Objects.requireNonNull(knowledgeGraph, "knowledgeGraph cannot be null");
        try (Session session = neo4jDriver.session(sessionConfig)) {
            return session.executeWrite(tx -> {
                tx.run(KnowledgeGraphCypherQueries.mergeGraph(), java.util.Map.of(
                    "graphId", knowledgeGraph.graphId().value(),
                    "properties", knowledgeGraphMapper.toNeo4jProperties(knowledgeGraph)
                ));
                for (KnowledgeNode node : knowledgeGraph.nodes()) {
                    tx.run(KnowledgeGraphCypherQueries.mergeNode(), java.util.Map.of(
                        "nodeId", node.nodeId().value(),
                        "properties", nodeMapper.toNeo4jProperties(node, knowledgeGraph.graphId())
                    ));
                    tx.run(KnowledgeGraphCypherQueries.mergeGraphContainsNode(), java.util.Map.of(
                        "graphId", knowledgeGraph.graphId().value(),
                        "nodeId", node.nodeId().value()
                    ));
                }
                for (KnowledgeRelationship relationship : knowledgeGraph.relationships()) {
                    tx.run(KnowledgeGraphCypherQueries.mergeRelationship(), java.util.Map.of(
                        "fromNodeId", relationship.fromNodeId().value(),
                        "toNodeId", relationship.toNodeId().value(),
                        "relationshipId", relationship.relationshipId().value(),
                        "properties", relationshipMapper.toNeo4jProperties(relationship)
                    ));
                }
                return knowledgeGraph;
            });
        } catch (RuntimeException exception) {
            throw new RepositoryException("Failed to save knowledge graph: " + knowledgeGraph.graphId(), exception);
        }
    }

    @Override
    public Optional<KnowledgeGraph> findById(GraphId graphId) {
        Objects.requireNonNull(graphId, "graphId cannot be null");
        try (Session session = neo4jDriver.session(sessionConfig)) {
            return session.executeRead(tx -> loadGraph(tx, graphId));
        } catch (RuntimeException exception) {
            throw new RepositoryException("Failed to find knowledge graph: " + graphId, exception);
        }
    }

    @Override
    public Optional<KnowledgeGraph> findByName(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        try (Session session = neo4jDriver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                Result result = tx.run(KnowledgeGraphCypherQueries.findGraphIdByName(), java.util.Map.of("name", name));
                if (!result.hasNext()) {
                    return Optional.empty();
                }
                return loadGraph(tx, new GraphId(result.next().get("graphId").asString()));
            });
        } catch (RuntimeException exception) {
            throw new RepositoryException("Failed to find knowledge graph by name: " + name, exception);
        }
    }

    @Override
    public List<KnowledgeGraph> findAll() {
        try (Session session = neo4jDriver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                List<KnowledgeGraph> graphs = new ArrayList<>();
                Result result = tx.run(KnowledgeGraphCypherQueries.findAllGraphIds(), java.util.Map.of());
                while (result.hasNext()) {
                    GraphId graphId = new GraphId(result.next().get("graphId").asString());
                    loadGraph(tx, graphId).ifPresent(graphs::add);
                }
                return List.copyOf(graphs);
            });
        } catch (RuntimeException exception) {
            throw new RepositoryException("Failed to find all knowledge graphs", exception);
        }
    }

    @Override
    public void delete(GraphId graphId) {
        Objects.requireNonNull(graphId, "graphId cannot be null");
        try (Session session = neo4jDriver.session(sessionConfig)) {
            session.executeWrite(tx -> {
                tx.run(KnowledgeGraphCypherQueries.deleteGraph(), java.util.Map.of("graphId", graphId.value()));
                return null;
            });
        } catch (RuntimeException exception) {
            throw new RepositoryException("Failed to delete knowledge graph: " + graphId, exception);
        }
    }

    @Override
    public boolean existsByName(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        try (Session session = neo4jDriver.session(sessionConfig)) {
            return session.executeRead(tx -> tx.run(KnowledgeGraphCypherQueries.graphExistsByName(), java.util.Map.of("name", name))
                .single().get("total").asInt() > 0);
        } catch (RuntimeException exception) {
            throw new RepositoryException("Failed to check knowledge graph existence by name: " + name, exception);
        }
    }

    @Override
    public List<KnowledgeNode> findNodesConnectedTo(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "nodeId cannot be null");
        try (Session session = neo4jDriver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                List<KnowledgeNode> nodes = new ArrayList<>();
                Result result = tx.run(KnowledgeGraphCypherQueries.findConnectedNodes(), java.util.Map.of("nodeId", nodeId.value()));
                while (result.hasNext()) {
                    nodes.add(nodeMapper.fromNeo4jNode(result.next().get("connected").asNode()));
                }
                return List.copyOf(nodes);
            });
        } catch (RuntimeException exception) {
            throw new RepositoryException("Failed to find nodes connected to: " + nodeId, exception);
        }
    }

    @Override
    public List<KnowledgeRelationship> findRelationshipsByType(RelationshipType type) {
        Objects.requireNonNull(type, "type cannot be null");
        try (Session session = neo4jDriver.session(sessionConfig)) {
            return session.executeRead(tx -> mapRelationships(
                tx.run(KnowledgeGraphCypherQueries.findRelationshipsByType(), java.util.Map.of("relationshipType", type.name()))
            ));
        } catch (RuntimeException exception) {
            throw new RepositoryException("Failed to find relationships by type: " + type, exception);
        }
    }

    @Override
    public KnowledgeGraph findSubgraphAroundNode(NodeId nodeId, int depth) {
        Objects.requireNonNull(nodeId, "nodeId cannot be null");
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be at least 1");
        }
        try (Session session = neo4jDriver.session(sessionConfig)) {
            return session.executeRead(tx -> {
                Set<KnowledgeNode> nodes = new LinkedHashSet<>();
                Result nodeResult = tx.run(KnowledgeGraphCypherQueries.findSubgraphNodes(), java.util.Map.of(
                    "nodeId", nodeId.value(),
                    "depth", depth
                ));
                while (nodeResult.hasNext()) {
                    nodes.add(nodeMapper.fromNeo4jNode(nodeResult.next().get("node").asNode()));
                }
                if (nodes.isEmpty()) {
                    throw new RepositoryException("No subgraph found around node: " + nodeId);
                }
                List<String> nodeIds = nodes.stream().map(node -> node.nodeId().value()).toList();
                Set<KnowledgeRelationship> relationships = new LinkedHashSet<>(mapRelationships(
                    tx.run(KnowledgeGraphCypherQueries.findSubgraphRelationships(), java.util.Map.of("nodeIds", nodeIds))
                ));
                return new KnowledgeGraph(
                    GraphId.generate(),
                    "subgraph-" + nodeId.value(),
                    nodes,
                    relationships,
                    com.rag.app.shared.domain.knowledge.valueobjects.GraphMetadata.empty(),
                    java.time.Instant.now(),
                    java.time.Instant.now()
                );
            });
        } catch (RepositoryException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RepositoryException("Failed to find subgraph around node: " + nodeId, exception);
        }
    }

    private Optional<KnowledgeGraph> loadGraph(TransactionContext tx, GraphId graphId) {
        Result graphResult = tx.run(KnowledgeGraphCypherQueries.findGraphById(), java.util.Map.of("graphId", graphId.value()));
        if (!graphResult.hasNext()) {
            return Optional.empty();
        }
        Record graphRecord = graphResult.next();
        Set<KnowledgeNode> nodes = new LinkedHashSet<>();
        Result nodeResult = tx.run(KnowledgeGraphCypherQueries.findNodesByGraphId(), java.util.Map.of("graphId", graphId.value()));
        while (nodeResult.hasNext()) {
            nodes.add(nodeMapper.fromNeo4jNode(nodeResult.next().get("n").asNode()));
        }
        Set<KnowledgeRelationship> relationships = new LinkedHashSet<>(mapRelationships(
            tx.run(KnowledgeGraphCypherQueries.findRelationshipsByGraphId(), java.util.Map.of("graphId", graphId.value()))
        ));
        return Optional.of(knowledgeGraphMapper.fromNeo4jRecord(graphRecord, nodes, relationships));
    }

    private List<KnowledgeRelationship> mapRelationships(Result result) {
        List<KnowledgeRelationship> relationships = new ArrayList<>();
        while (result.hasNext()) {
            Record record = result.next();
            relationships.add(relationshipMapper.fromNeo4jRelationship(
                record.get("r").asRelationship(),
                record.get("fromNodeId").asString(),
                record.get("toNodeId").asString()
            ));
        }
        return List.copyOf(relationships);
    }
}

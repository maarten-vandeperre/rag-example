package com.rag.app.shared.interfaces.knowledge;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;

import java.util.List;
import java.util.Optional;

public interface KnowledgeGraphRepository {
    KnowledgeGraph save(KnowledgeGraph knowledgeGraph);

    Optional<KnowledgeGraph> findById(GraphId graphId);

    Optional<KnowledgeGraph> findByName(String name);

    List<KnowledgeGraph> findAll();

    void delete(GraphId graphId);

    boolean existsByName(String name);

    List<KnowledgeNode> findNodesConnectedTo(NodeId nodeId);

    List<KnowledgeRelationship> findRelationshipsByType(RelationshipType type);

    KnowledgeGraph findSubgraphAroundNode(NodeId nodeId, int depth);
}

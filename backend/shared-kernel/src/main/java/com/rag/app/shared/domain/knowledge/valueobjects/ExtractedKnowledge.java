package com.rag.app.shared.domain.knowledge.valueobjects;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;

import java.util.List;
import java.util.Objects;

public record ExtractedKnowledge(List<KnowledgeNode> nodes,
                                 List<KnowledgeRelationship> relationships,
                                 DocumentReference sourceDocument,
                                 ExtractionMetadata metadata) {
    public ExtractedKnowledge {
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes cannot be null"));
        relationships = List.copyOf(Objects.requireNonNull(relationships, "relationships cannot be null"));
        Objects.requireNonNull(sourceDocument, "sourceDocument cannot be null");
        Objects.requireNonNull(metadata, "metadata cannot be null");
    }

    public boolean isEmpty() {
        return nodes.isEmpty() && relationships.isEmpty();
    }

    public int totalElements() {
        return nodes.size() + relationships.size();
    }
}

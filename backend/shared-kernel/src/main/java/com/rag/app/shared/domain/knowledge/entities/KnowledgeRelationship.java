package com.rag.app.shared.domain.knowledge.entities;

import com.rag.app.shared.domain.exceptions.BusinessRuleViolationException;
import com.rag.app.shared.domain.knowledge.valueobjects.ConfidenceScore;
import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipId;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class KnowledgeRelationship {
    private final RelationshipId relationshipId;
    private final NodeId fromNodeId;
    private final NodeId toNodeId;
    private final RelationshipType relationshipType;
    private final Map<String, Object> properties;
    private final DocumentReference sourceDocument;
    private final ConfidenceScore confidence;
    private final Instant createdAt;

    public KnowledgeRelationship(RelationshipId relationshipId,
                                 NodeId fromNodeId,
                                 NodeId toNodeId,
                                 RelationshipType relationshipType,
                                 Map<String, Object> properties,
                                 DocumentReference sourceDocument,
                                 ConfidenceScore confidence,
                                 Instant createdAt) {
        this.relationshipId = Objects.requireNonNull(relationshipId, "relationshipId cannot be null");
        this.fromNodeId = Objects.requireNonNull(fromNodeId, "fromNodeId cannot be null");
        this.toNodeId = Objects.requireNonNull(toNodeId, "toNodeId cannot be null");
        if (fromNodeId.equals(toNodeId)) {
            throw new BusinessRuleViolationException("relationship_distinct_nodes", "relationships must connect two distinct nodes");
        }
        this.relationshipType = Objects.requireNonNull(relationshipType, "relationshipType cannot be null");
        this.properties = Map.copyOf(Objects.requireNonNull(properties, "properties cannot be null"));
        this.sourceDocument = Objects.requireNonNull(sourceDocument, "sourceDocument cannot be null");
        this.confidence = Objects.requireNonNull(confidence, "confidence cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public static KnowledgeRelationship create(NodeId fromNodeId,
                                               NodeId toNodeId,
                                               RelationshipType relationshipType,
                                               Map<String, Object> properties,
                                               DocumentReference sourceDocument,
                                               ConfidenceScore confidence,
                                               Instant createdAt) {
        return new KnowledgeRelationship(RelationshipId.generate(), fromNodeId, toNodeId, relationshipType, properties, sourceDocument, confidence, createdAt);
    }

    public RelationshipId relationshipId() {
        return relationshipId;
    }

    public NodeId fromNodeId() {
        return fromNodeId;
    }

    public NodeId toNodeId() {
        return toNodeId;
    }

    public RelationshipType relationshipType() {
        return relationshipType;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    public DocumentReference sourceDocument() {
        return sourceDocument;
    }

    public ConfidenceScore confidence() {
        return confidence;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean references(NodeId nodeId) {
        return fromNodeId.equals(nodeId) || toNodeId.equals(nodeId);
    }

    public boolean connects(NodeId candidateFromNodeId, NodeId candidateToNodeId) {
        return fromNodeId.equals(candidateFromNodeId) && toNodeId.equals(candidateToNodeId);
    }

    public KnowledgeRelationship withEndpoints(NodeId updatedFromNodeId, NodeId updatedToNodeId) {
        return new KnowledgeRelationship(relationshipId, updatedFromNodeId, updatedToNodeId, relationshipType, properties, sourceDocument, confidence, createdAt);
    }

    public KnowledgeRelationship withProperties(Map<String, Object> additionalProperties) {
        Map<String, Object> mergedProperties = new LinkedHashMap<>(properties);
        mergedProperties.putAll(Objects.requireNonNull(additionalProperties, "additionalProperties cannot be null"));
        return new KnowledgeRelationship(relationshipId, fromNodeId, toNodeId, relationshipType, mergedProperties, sourceDocument, confidence, createdAt);
    }

    public KnowledgeRelationship withConfidence(ConfidenceScore updatedConfidence) {
        return new KnowledgeRelationship(relationshipId, fromNodeId, toNodeId, relationshipType, properties, sourceDocument,
            Objects.requireNonNull(updatedConfidence, "updatedConfidence cannot be null"), createdAt);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof KnowledgeRelationship that)) {
            return false;
        }
        return relationshipId.equals(that.relationshipId);
    }

    @Override
    public int hashCode() {
        return relationshipId.hashCode();
    }
}

package com.rag.app.shared.domain.knowledge.entities;

import com.rag.app.shared.domain.exceptions.ValidationException;
import com.rag.app.shared.domain.knowledge.valueobjects.ConfidenceScore;
import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class KnowledgeNode {
    private final NodeId nodeId;
    private final String label;
    private final NodeType nodeType;
    private final Map<String, Object> properties;
    private final DocumentReference sourceDocument;
    private final ConfidenceScore confidence;
    private final Instant createdAt;
    private final Instant lastUpdatedAt;

    public KnowledgeNode(NodeId nodeId,
                         String label,
                         NodeType nodeType,
                         Map<String, Object> properties,
                         DocumentReference sourceDocument,
                         ConfidenceScore confidence,
                         Instant createdAt,
                         Instant lastUpdatedAt) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId cannot be null");
        if (label == null || label.isBlank()) {
            throw new ValidationException("label cannot be null or blank");
        }
        this.nodeType = Objects.requireNonNull(nodeType, "nodeType cannot be null");
        this.properties = Map.copyOf(Objects.requireNonNull(properties, "properties cannot be null"));
        this.sourceDocument = Objects.requireNonNull(sourceDocument, "sourceDocument cannot be null");
        this.confidence = Objects.requireNonNull(confidence, "confidence cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.lastUpdatedAt = Objects.requireNonNull(lastUpdatedAt, "lastUpdatedAt cannot be null");
        if (lastUpdatedAt.isBefore(createdAt)) {
            throw new ValidationException("lastUpdatedAt cannot be before createdAt");
        }
        this.label = label.trim();
    }

    public static KnowledgeNode create(String label,
                                       NodeType nodeType,
                                       Map<String, Object> properties,
                                       DocumentReference sourceDocument,
                                       ConfidenceScore confidence,
                                       Instant createdAt) {
        return new KnowledgeNode(NodeId.generate(), label, nodeType, properties, sourceDocument, confidence, createdAt, createdAt);
    }

    public NodeId nodeId() {
        return nodeId;
    }

    public String label() {
        return label;
    }

    public NodeType nodeType() {
        return nodeType;
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

    public Instant lastUpdatedAt() {
        return lastUpdatedAt;
    }

    public boolean matchesLabel(String candidateLabel) {
        return candidateLabel != null && label.equalsIgnoreCase(candidateLabel.trim());
    }

    public KnowledgeNode withProperties(Map<String, Object> additionalProperties, Instant updatedAt) {
        validateUpdatedAt(updatedAt);
        Map<String, Object> mergedProperties = new LinkedHashMap<>(properties);
        mergedProperties.putAll(Objects.requireNonNull(additionalProperties, "additionalProperties cannot be null"));
        return new KnowledgeNode(nodeId, label, nodeType, mergedProperties, sourceDocument, confidence, createdAt, updatedAt);
    }

    public KnowledgeNode withConfidence(ConfidenceScore updatedConfidence, Instant updatedAt) {
        validateUpdatedAt(updatedAt);
        return new KnowledgeNode(nodeId, label, nodeType, properties, sourceDocument,
            Objects.requireNonNull(updatedConfidence, "updatedConfidence cannot be null"), createdAt, updatedAt);
    }

    private void validateUpdatedAt(Instant updatedAt) {
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        if (updatedAt.isBefore(lastUpdatedAt)) {
            throw new ValidationException("updatedAt cannot move backwards in time");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof KnowledgeNode that)) {
            return false;
        }
        return nodeId.equals(that.nodeId);
    }

    @Override
    public int hashCode() {
        return nodeId.hashCode();
    }
}

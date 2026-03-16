package com.rag.app.shared.domain.knowledge.entities;

import com.rag.app.shared.domain.exceptions.BusinessRuleViolationException;
import com.rag.app.shared.domain.exceptions.ValidationException;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphMetadata;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipId;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class KnowledgeGraph {
    private final GraphId graphId;
    private final String name;
    private final Set<KnowledgeNode> nodes;
    private final Set<KnowledgeRelationship> relationships;
    private final GraphMetadata metadata;
    private final Instant createdAt;
    private final Instant lastUpdatedAt;

    public KnowledgeGraph(GraphId graphId,
                          String name,
                          Set<KnowledgeNode> nodes,
                          Set<KnowledgeRelationship> relationships,
                          GraphMetadata metadata,
                          Instant createdAt,
                          Instant lastUpdatedAt) {
        this.graphId = Objects.requireNonNull(graphId, "graphId cannot be null");
        if (name == null || name.isBlank()) {
            throw new ValidationException("name cannot be null or blank");
        }
        this.nodes = Set.copyOf(Objects.requireNonNull(nodes, "nodes cannot be null"));
        this.relationships = Set.copyOf(Objects.requireNonNull(relationships, "relationships cannot be null"));
        this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.lastUpdatedAt = Objects.requireNonNull(lastUpdatedAt, "lastUpdatedAt cannot be null");
        if (lastUpdatedAt.isBefore(createdAt)) {
            throw new ValidationException("lastUpdatedAt cannot be before createdAt");
        }
        this.name = name.trim();
        validateRelationshipReferences(this.nodes, this.relationships);
    }

    public static KnowledgeGraph create(String name, GraphMetadata metadata, Instant createdAt) {
        return new KnowledgeGraph(GraphId.generate(), name, Set.of(), Set.of(), metadata, createdAt, createdAt);
    }

    public GraphId graphId() {
        return graphId;
    }

    public String name() {
        return name;
    }

    public Set<KnowledgeNode> nodes() {
        return nodes;
    }

    public Set<KnowledgeRelationship> relationships() {
        return relationships;
    }

    public GraphMetadata metadata() {
        return metadata;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastUpdatedAt() {
        return lastUpdatedAt;
    }

    public boolean containsNode(NodeId nodeId) {
        return nodes.stream().anyMatch(node -> node.nodeId().equals(nodeId));
    }

    public boolean containsRelationship(RelationshipId relationshipId) {
        return relationships.stream().anyMatch(relationship -> relationship.relationshipId().equals(relationshipId));
    }

    public Optional<KnowledgeNode> getNode(NodeId nodeId) {
        return nodes.stream().filter(node -> node.nodeId().equals(nodeId)).findFirst();
    }

    public Set<KnowledgeRelationship> relationshipsFor(NodeId nodeId) {
        return relationships.stream()
            .filter(relationship -> relationship.references(nodeId))
            .collect(Collectors.toUnmodifiableSet());
    }

    public KnowledgeGraph addNode(KnowledgeNode node, Instant updatedAt) {
        validateUpdatedAt(updatedAt);
        Objects.requireNonNull(node, "node cannot be null");
        Set<KnowledgeNode> updatedNodes = new LinkedHashSet<>(nodes);
        updatedNodes.remove(node);
        updatedNodes.add(node);
        return new KnowledgeGraph(graphId, name, updatedNodes, relationships, metadata, createdAt, updatedAt);
    }

    public KnowledgeGraph replaceNode(KnowledgeNode node, Instant updatedAt) {
        if (!containsNode(node.nodeId())) {
            throw new BusinessRuleViolationException("graph_node_exists", "cannot replace a node that is not present in the graph");
        }
        return addNode(node, updatedAt);
    }

    public KnowledgeGraph addRelationship(KnowledgeRelationship relationship, Instant updatedAt) {
        validateUpdatedAt(updatedAt);
        Objects.requireNonNull(relationship, "relationship cannot be null");
        if (!containsNode(relationship.fromNodeId()) || !containsNode(relationship.toNodeId())) {
            throw new BusinessRuleViolationException("graph_relationship_nodes_exist", "relationship endpoints must exist in the graph");
        }
        Set<KnowledgeRelationship> updatedRelationships = new LinkedHashSet<>(relationships);
        updatedRelationships.remove(relationship);
        updatedRelationships.add(relationship);
        return new KnowledgeGraph(graphId, name, nodes, updatedRelationships, metadata, createdAt, updatedAt);
    }

    public KnowledgeGraph replaceRelationship(KnowledgeRelationship relationship, Instant updatedAt) {
        if (!containsRelationship(relationship.relationshipId())) {
            throw new BusinessRuleViolationException("graph_relationship_exists", "cannot replace a relationship that is not present in the graph");
        }
        return addRelationship(relationship, updatedAt);
    }

    public KnowledgeGraph removeNode(NodeId nodeId, Instant updatedAt) {
        validateUpdatedAt(updatedAt);
        Set<KnowledgeNode> updatedNodes = nodes.stream()
            .filter(node -> !node.nodeId().equals(nodeId))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<KnowledgeRelationship> updatedRelationships = relationships.stream()
            .filter(relationship -> !relationship.references(nodeId))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return new KnowledgeGraph(graphId, name, updatedNodes, updatedRelationships, metadata, createdAt, updatedAt);
    }

    public KnowledgeGraph mergeWith(KnowledgeGraph other, Instant updatedAt) {
        validateUpdatedAt(updatedAt);
        Objects.requireNonNull(other, "other cannot be null");
        Set<KnowledgeNode> mergedNodes = new LinkedHashSet<>(nodes);
        mergedNodes.addAll(other.nodes);
        Set<KnowledgeRelationship> mergedRelationships = new LinkedHashSet<>(relationships);
        mergedRelationships.addAll(other.relationships);
        Map<String, Object> mergedAttributes = new LinkedHashMap<>(metadata.attributes());
        mergedAttributes.putAll(other.metadata.attributes());
        Set<com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference> mergedSourceDocuments = new LinkedHashSet<>(metadata.sourceDocuments());
        mergedSourceDocuments.addAll(other.metadata.sourceDocuments());
        GraphMetadata mergedMetadata = new GraphMetadata(
            metadata.description().isBlank() ? other.metadata.description() : metadata.description(),
            mergedSourceDocuments,
            mergedAttributes
        );
        return new KnowledgeGraph(graphId, name, mergedNodes, mergedRelationships, mergedMetadata, createdAt, updatedAt);
    }

    private void validateUpdatedAt(Instant updatedAt) {
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        if (updatedAt.isBefore(lastUpdatedAt)) {
            throw new ValidationException("updatedAt cannot move backwards in time");
        }
    }

    private static void validateRelationshipReferences(Set<KnowledgeNode> nodes, Set<KnowledgeRelationship> relationships) {
        Set<NodeId> nodeIds = nodes.stream()
            .map(KnowledgeNode::nodeId)
            .collect(Collectors.toSet());
        for (KnowledgeRelationship relationship : relationships) {
            if (!nodeIds.contains(relationship.fromNodeId()) || !nodeIds.contains(relationship.toNodeId())) {
                throw new BusinessRuleViolationException("graph_relationship_nodes_exist", "all relationships must reference nodes that exist in the graph");
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof KnowledgeGraph that)) {
            return false;
        }
        return graphId.equals(that.graphId);
    }

    @Override
    public int hashCode() {
        return graphId.hashCode();
    }
}

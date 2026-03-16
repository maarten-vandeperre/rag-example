package com.rag.app.shared.domain.knowledge.services;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.valueobjects.ConfidenceScore;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class KnowledgeGraphDomainService {
    public KnowledgeGraph mergeExtractedKnowledge(KnowledgeGraph existingGraph, ExtractedKnowledge extractedKnowledge) {
        Objects.requireNonNull(existingGraph, "existingGraph cannot be null");
        Objects.requireNonNull(extractedKnowledge, "extractedKnowledge cannot be null");

        validateGraphConsistency(existingGraph);

        KnowledgeGraph mergedGraph = existingGraph;
        Map<NodeId, NodeId> nodeRemapping = new LinkedHashMap<>();

        for (KnowledgeNode candidateNode : extractedKnowledge.nodes()) {
            Optional<KnowledgeNode> existingNode = mergedGraph.nodes().stream()
                .filter(node -> shouldMergeNodes(node, candidateNode))
                .findFirst();

            if (existingNode.isPresent()) {
                KnowledgeNode mergedNode = mergeNodes(existingNode.get(), candidateNode);
                mergedGraph = mergedGraph.replaceNode(mergedNode, Instant.now());
                nodeRemapping.put(candidateNode.nodeId(), mergedNode.nodeId());
            } else {
                mergedGraph = mergedGraph.addNode(candidateNode, Instant.now());
                nodeRemapping.put(candidateNode.nodeId(), candidateNode.nodeId());
            }
        }

        for (KnowledgeRelationship candidateRelationship : extractedKnowledge.relationships()) {
            NodeId remappedFromNodeId = nodeRemapping.getOrDefault(candidateRelationship.fromNodeId(), candidateRelationship.fromNodeId());
            NodeId remappedToNodeId = nodeRemapping.getOrDefault(candidateRelationship.toNodeId(), candidateRelationship.toNodeId());
            if (remappedFromNodeId.equals(remappedToNodeId)) {
                continue;
            }

            KnowledgeRelationship remappedRelationship = candidateRelationship.withEndpoints(remappedFromNodeId, remappedToNodeId);
            Optional<KnowledgeRelationship> existingRelationship = mergedGraph.relationships().stream()
                .filter(relationship -> isEquivalentRelationship(relationship, remappedRelationship))
                .findFirst();

            if (existingRelationship.isPresent()) {
                KnowledgeRelationship mergedRelationship = mergeRelationships(existingRelationship.get(), remappedRelationship);
                mergedGraph = mergedGraph.replaceRelationship(mergedRelationship, Instant.now());
            } else {
                mergedGraph = mergedGraph.addRelationship(remappedRelationship, Instant.now());
            }
        }

        validateGraphConsistency(mergedGraph);
        return mergedGraph;
    }

    public boolean shouldMergeNodes(KnowledgeNode existing, KnowledgeNode candidate) {
        Objects.requireNonNull(existing, "existing cannot be null");
        Objects.requireNonNull(candidate, "candidate cannot be null");
        return existing.nodeType() == candidate.nodeType()
            && existing.label().trim().equalsIgnoreCase(candidate.label().trim());
    }

    public KnowledgeNode mergeNodes(KnowledgeNode primary, KnowledgeNode secondary) {
        Objects.requireNonNull(primary, "primary cannot be null");
        Objects.requireNonNull(secondary, "secondary cannot be null");

        Map<String, Object> mergedProperties = mergeProperties(primary.properties(), secondary.properties());
        ConfidenceScore mergedConfidence = primary.confidence().mergeWith(secondary.confidence());
        Instant updatedAt = primary.lastUpdatedAt().isAfter(secondary.lastUpdatedAt())
            ? primary.lastUpdatedAt()
            : secondary.lastUpdatedAt();

        String mergedLabel = primary.label().length() >= secondary.label().length()
            ? primary.label()
            : secondary.label();

        return new KnowledgeNode(
            primary.nodeId(),
            mergedLabel,
            primary.nodeType(),
            mergedProperties,
            primary.sourceDocument(),
            mergedConfidence,
            primary.createdAt(),
            updatedAt
        );
    }

    public void validateGraphConsistency(KnowledgeGraph graph) {
        Objects.requireNonNull(graph, "graph cannot be null");

        Set<NodeId> nodeIds = new LinkedHashSet<>();
        for (KnowledgeNode node : graph.nodes()) {
            nodeIds.add(node.nodeId());
            Objects.requireNonNull(node.confidence(), "node confidence cannot be null");
        }

        for (KnowledgeRelationship relationship : graph.relationships()) {
            if (!nodeIds.contains(relationship.fromNodeId()) || !nodeIds.contains(relationship.toNodeId())) {
                throw new IllegalStateException("Relationship references nodes that do not exist in the graph");
            }
            Objects.requireNonNull(relationship.confidence(), "relationship confidence cannot be null");
        }
    }

    private boolean isEquivalentRelationship(KnowledgeRelationship existing, KnowledgeRelationship candidate) {
        return existing.fromNodeId().equals(candidate.fromNodeId())
            && existing.toNodeId().equals(candidate.toNodeId())
            && existing.relationshipType() == candidate.relationshipType();
    }

    private KnowledgeRelationship mergeRelationships(KnowledgeRelationship primary, KnowledgeRelationship secondary) {
        return primary
            .withProperties(mergeProperties(primary.properties(), secondary.properties()))
            .withConfidence(primary.confidence().mergeWith(secondary.confidence()));
    }

    private Map<String, Object> mergeProperties(Map<String, Object> primary, Map<String, Object> secondary) {
        Map<String, Object> merged = new LinkedHashMap<>(primary);
        for (Map.Entry<String, Object> entry : secondary.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), this::mergePropertyValue);
        }
        return merged;
    }

    private Object mergePropertyValue(Object currentValue, Object newValue) {
        if (Objects.equals(currentValue, newValue)) {
            return currentValue;
        }

        List<Object> combinedValues = new ArrayList<>();
        appendPropertyValue(combinedValues, currentValue);
        appendPropertyValue(combinedValues, newValue);
        return List.copyOf(new LinkedHashSet<>(combinedValues));
    }

    private void appendPropertyValue(List<Object> target, Object value) {
        if (value instanceof Collection<?> collection) {
            target.addAll(collection);
            return;
        }
        target.add(value);
    }
}

package com.rag.app.shared.domain.knowledge;

import com.rag.app.shared.domain.exceptions.BusinessRuleViolationException;
import com.rag.app.shared.domain.exceptions.ValidationException;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.services.KnowledgeGraphDomainService;
import com.rag.app.shared.domain.knowledge.valueobjects.ConfidenceScore;
import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractionMetadata;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphMetadata;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeType;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeGraphDomainModelTest {
    private static final Instant CREATED_AT = Instant.parse("2026-03-16T10:15:30Z");
    private static final DocumentReference SOURCE_DOCUMENT = new DocumentReference(
        UUID.fromString("00000000-0000-0000-0000-000000000067"),
        "knowledge-graph.md",
        "section-1",
        0.92d
    );

    @Test
    void shouldCreateImmutableKnowledgeEntitiesAndGraphOperations() {
        KnowledgeNode topicNode = new KnowledgeNode(
            new NodeId("topic-node"),
            "Knowledge Graph",
            NodeType.TOPIC,
            Map.of("keywords", List.of("graph", "semantic")),
            SOURCE_DOCUMENT,
            ConfidenceScore.high(),
            CREATED_AT,
            CREATED_AT
        );
        KnowledgeNode conceptNode = KnowledgeNode.create(
            "Entity Resolution",
            NodeType.CONCEPT,
            Map.of("stage", "enrichment"),
            SOURCE_DOCUMENT,
            ConfidenceScore.medium(),
            CREATED_AT.plusSeconds(1)
        );

        KnowledgeRelationship relationship = KnowledgeRelationship.create(
            topicNode.nodeId(),
            conceptNode.nodeId(),
            RelationshipType.RELATED_TO,
            Map.of("weight", 3),
            SOURCE_DOCUMENT,
            ConfidenceScore.medium(),
            CREATED_AT.plusSeconds(2)
        );

        KnowledgeGraph graph = KnowledgeGraph.create(
            "document knowledge",
            new GraphMetadata("Knowledge extracted from upload", Set.of(SOURCE_DOCUMENT), Map.of("origin", "upload-pipeline")),
            CREATED_AT
        ).addNode(topicNode, CREATED_AT.plusSeconds(3))
            .addNode(conceptNode, CREATED_AT.plusSeconds(4))
            .addRelationship(relationship, CREATED_AT.plusSeconds(5));

        KnowledgeNode enrichedNode = topicNode.withProperties(Map.of("aliases", List.of("KG")), CREATED_AT.plusSeconds(6));

        assertTrue(graph.containsNode(topicNode.nodeId()));
        assertTrue(graph.containsRelationship(relationship.relationshipId()));
        assertEquals(1, graph.relationshipsFor(topicNode.nodeId()).size());
        assertTrue(enrichedNode.matchesLabel("knowledge graph"));
        assertEquals(List.of("KG"), enrichedNode.properties().get("aliases"));
        assertFalse(topicNode.properties().containsKey("aliases"));
    }

    @Test
    void shouldMergeExtractedKnowledgeIntoExistingGraph() {
        KnowledgeGraphDomainService service = new KnowledgeGraphDomainService();

        KnowledgeNode existingPerson = new KnowledgeNode(
            new NodeId("person-1"),
            "Ada Lovelace",
            NodeType.PERSON,
            Map.of("role", "author"),
            SOURCE_DOCUMENT,
            new ConfidenceScore(0.61d),
            CREATED_AT,
            CREATED_AT
        );
        KnowledgeNode existingTopic = new KnowledgeNode(
            new NodeId("topic-1"),
            "Analytical Engine",
            NodeType.TOPIC,
            Map.of(),
            SOURCE_DOCUMENT,
            ConfidenceScore.high(),
            CREATED_AT.plusSeconds(1),
            CREATED_AT.plusSeconds(1)
        );

        KnowledgeGraph existingGraph = KnowledgeGraph.create("knowledge", GraphMetadata.empty(), CREATED_AT)
            .addNode(existingPerson, CREATED_AT.plusSeconds(2))
            .addNode(existingTopic, CREATED_AT.plusSeconds(3));

        KnowledgeNode extractedPerson = new KnowledgeNode(
            new NodeId("person-2"),
            " ada lovelace ",
            NodeType.PERSON,
            Map.of("aliases", List.of("Countess of Lovelace")),
            SOURCE_DOCUMENT,
            ConfidenceScore.high(),
            CREATED_AT.plusSeconds(4),
            CREATED_AT.plusSeconds(4)
        );
        KnowledgeNode extractedConcept = new KnowledgeNode(
            new NodeId("concept-1"),
            "Algorithm",
            NodeType.CONCEPT,
            Map.of("importance", "high"),
            SOURCE_DOCUMENT,
            ConfidenceScore.medium(),
            CREATED_AT.plusSeconds(5),
            CREATED_AT.plusSeconds(5)
        );
        KnowledgeRelationship extractedRelationship = KnowledgeRelationship.create(
            extractedPerson.nodeId(),
            extractedConcept.nodeId(),
            RelationshipType.RELATED_TO,
            Map.of("evidence", "paragraph-4"),
            SOURCE_DOCUMENT,
            ConfidenceScore.medium(),
            CREATED_AT.plusSeconds(6)
        );

        ExtractedKnowledge extractedKnowledge = new ExtractedKnowledge(
            List.of(extractedPerson, extractedConcept),
            List.of(extractedRelationship),
            SOURCE_DOCUMENT,
            new ExtractionMetadata("llm", CREATED_AT.plusSeconds(7), Duration.ofSeconds(4), Map.of("model", "test"), List.of())
        );

        KnowledgeGraph mergedGraph = service.mergeExtractedKnowledge(existingGraph, extractedKnowledge);

        KnowledgeNode mergedPerson = mergedGraph.getNode(existingPerson.nodeId()).orElseThrow();
        assertEquals(3, mergedGraph.nodes().size());
        assertEquals(1, mergedGraph.relationships().size());
        assertEquals(List.of("Countess of Lovelace"), mergedPerson.properties().get("aliases"));
        assertEquals(0.9d, mergedPerson.confidence().value());
        assertTrue(mergedGraph.relationships().stream().allMatch(relationship -> relationship.fromNodeId().equals(existingPerson.nodeId())));
    }

    @Test
    void shouldValidateKnowledgeValueObjectsAndExtractionModels() {
        ConfidenceScore low = ConfidenceScore.low();
        ConfidenceScore medium = ConfidenceScore.medium();

        ExtractionMetadata metadata = new ExtractionMetadata(
            "rule-engine",
            CREATED_AT,
            Duration.ofSeconds(2),
            Map.of("window", 5),
            List.of("missing optional taxonomy")
        );
        ExtractedKnowledge extractedKnowledge = new ExtractedKnowledge(List.of(), List.of(), SOURCE_DOCUMENT, metadata);

        assertTrue(low.isLowConfidence());
        assertTrue(medium.isMediumConfidence());
        assertTrue(metadata.hasWarnings());
        assertTrue(extractedKnowledge.isEmpty());
        assertEquals(0, extractedKnowledge.totalElements());
        assertTrue(SOURCE_DOCUMENT.isHighlyRelevant());

        ValidationException invalidConfidence = assertThrows(ValidationException.class, () -> new ConfidenceScore(1.2d));
        ValidationException invalidDocument = assertThrows(ValidationException.class,
            () -> new DocumentReference(UUID.randomUUID(), " ", "section", 0.4d));

        assertEquals("Confidence score must be between 0.0 and 1.0", invalidConfidence.getMessage());
        assertEquals("documentName cannot be null or blank", invalidDocument.getMessage());
    }

    @Test
    void shouldRejectInconsistentGraphsAndRelationships() {
        KnowledgeNode node = new KnowledgeNode(
            new NodeId("node-1"),
            "Invalid Graph",
            NodeType.TOPIC,
            Map.of(),
            SOURCE_DOCUMENT,
            ConfidenceScore.high(),
            CREATED_AT,
            CREATED_AT
        );

        BusinessRuleViolationException invalidRelationship = assertThrows(BusinessRuleViolationException.class,
            () -> KnowledgeRelationship.create(node.nodeId(), node.nodeId(), RelationshipType.RELATED_TO, Map.of(), SOURCE_DOCUMENT, ConfidenceScore.low(), CREATED_AT));

        BusinessRuleViolationException missingRelationshipNodes = assertThrows(BusinessRuleViolationException.class,
            () -> new KnowledgeGraph(
                com.rag.app.shared.domain.knowledge.valueobjects.GraphId.generate(),
                "broken",
                Set.of(node),
                Set.of(KnowledgeRelationship.create(node.nodeId(), new NodeId("missing"), RelationshipType.RELATED_TO, Map.of(), SOURCE_DOCUMENT, ConfidenceScore.low(), CREATED_AT.plusSeconds(1))),
                GraphMetadata.empty(),
                CREATED_AT,
                CREATED_AT.plusSeconds(1)
            ));

        assertTrue(invalidRelationship.getMessage().contains("relationships must connect two distinct nodes"));
        assertTrue(missingRelationshipNodes.getMessage().contains("all relationships must reference nodes that exist in the graph"));
    }

    @Test
    void shouldValidateLargeGraphConsistency() {
        KnowledgeGraphDomainService service = new KnowledgeGraphDomainService();
        KnowledgeGraph graph = KnowledgeGraph.create("large-graph", GraphMetadata.empty(), CREATED_AT);
        KnowledgeNode previousNode = null;

        long graphUpdateOffset = 1_000L;
        for (int index = 0; index < 250; index++) {
            KnowledgeNode currentNode = new KnowledgeNode(
                new NodeId("node-" + index),
                "Topic-" + index,
                NodeType.TOPIC,
                Map.of("rank", index),
                SOURCE_DOCUMENT,
                ConfidenceScore.medium(),
                CREATED_AT.plusSeconds(index),
                CREATED_AT.plusSeconds(index)
            );
            graph = graph.addNode(currentNode, CREATED_AT.plusSeconds(graphUpdateOffset++));

            if (previousNode != null) {
                graph = graph.addRelationship(
                    KnowledgeRelationship.create(previousNode.nodeId(), currentNode.nodeId(), RelationshipType.RELATED_TO, Map.of("index", index), SOURCE_DOCUMENT, ConfidenceScore.low(), CREATED_AT.plusSeconds(index + 600)),
                    CREATED_AT.plusSeconds(graphUpdateOffset++)
                );
            }

            previousNode = currentNode;
        }

        service.validateGraphConsistency(graph);

        assertEquals(250, graph.nodes().size());
        assertEquals(249, graph.relationships().size());
    }
}

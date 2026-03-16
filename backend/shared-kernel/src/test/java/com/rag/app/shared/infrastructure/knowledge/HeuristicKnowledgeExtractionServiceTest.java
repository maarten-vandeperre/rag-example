package com.rag.app.shared.infrastructure.knowledge;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeType;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeuristicKnowledgeExtractionServiceTest {
    private static final String SAMPLE_MARKDOWN = """
        # Knowledge Graph Overview
        Ada Lovelace works for Analytical Engine Labs in London.
        The knowledge graph pipeline improves semantic search and entity extraction.

        ## Document Upload
        Analytical Engine Labs is located in London.
        The document upload pipeline mentions Neo4j Corporation and knowledge extraction.
        """;

    @Test
    void shouldExtractEntitiesRelationshipsAndSectionsFromMarkdown() {
        HeuristicKnowledgeExtractionService service = new HeuristicKnowledgeExtractionService();

        var extractedKnowledge = service.extractKnowledge(
            SAMPLE_MARKDOWN,
            "knowledge.md",
            "markdown",
            Map.of("strategy", "thorough", "min_confidence", 0.60d)
        );

        assertTrue(extractedKnowledge.nodes().stream().anyMatch(node -> node.nodeType() == NodeType.PERSON && node.label().equals("Ada Lovelace")));
        assertTrue(extractedKnowledge.nodes().stream().anyMatch(node -> node.nodeType() == NodeType.ORGANIZATION && node.label().equals("Analytical Engine Labs")));
        assertTrue(extractedKnowledge.nodes().stream().anyMatch(node -> node.nodeType() == NodeType.LOCATION && node.label().equals("London")));
        assertTrue(extractedKnowledge.nodes().stream().anyMatch(node -> node.nodeType() == NodeType.DOCUMENT_SECTION));
        assertTrue(extractedKnowledge.relationships().stream().anyMatch(relationship -> "works_for".equals(relationship.properties().get("predicate"))));
        assertTrue(extractedKnowledge.relationships().stream().anyMatch(relationship -> relationship.relationshipType().name().equals("MENTIONS")));
        assertEquals("heuristic-pattern-extraction", extractedKnowledge.metadata().extractionMethod());
    }

    @Test
    void shouldRespectConfidenceThresholdsAndChunkLargeDocuments() {
        HeuristicKnowledgeExtractionService service = new HeuristicKnowledgeExtractionService();
        String largeContent = SAMPLE_MARKDOWN.repeat(8);

        var extractedKnowledge = service.extractKnowledge(
            largeContent,
            "large-knowledge.md",
            "markdown",
            Map.of("chunk_size", 200, "min_confidence", 0.85d, "strategy", "fast")
        );

        assertTrue(extractedKnowledge.metadata().warnings().stream().anyMatch(warning -> warning.contains("chunks")));
        assertTrue(extractedKnowledge.nodes().stream().allMatch(node -> node.confidence().value() >= 0.85d));
        assertTrue(extractedKnowledge.nodes().size() < 10);
    }

    @Test
    void shouldValidateDocumentQualityHeuristically() {
        HeuristicDocumentQualityValidator validator = new HeuristicDocumentQualityValidator();

        DocumentQualityResult strongDocument = validator.validateForKnowledgeExtraction(SAMPLE_MARKDOWN, "markdown");
        DocumentQualityResult weakDocument = validator.validateForKnowledgeExtraction("12345 67890", "plain_text");

        assertTrue(strongDocument.sufficientForExtraction());
        assertFalse(weakDocument.sufficientForExtraction());
        assertTrue(weakDocument.issues().stream().anyMatch(issue -> issue.contains("too short") || issue.contains("English-like")));
        assertTrue(validator.hasStructuredContent(SAMPLE_MARKDOWN, "markdown"));
    }

    @Test
    void shouldExposeSupportedExtractionTypesAndDocumentFormats() {
        HeuristicKnowledgeExtractionService service = new HeuristicKnowledgeExtractionService();

        assertTrue(service.supportsDocumentType("markdown"));
        assertTrue(service.supportsDocumentType("PDF"));
        assertFalse(service.supportsDocumentType("docx"));
        assertTrue(service.getSupportedExtractionTypes().containsAll(List.of("entities", "relationships", "sections")));
        assertTrue(service.getDefaultExtractionOptions().containsKey("min_confidence"));
    }
}

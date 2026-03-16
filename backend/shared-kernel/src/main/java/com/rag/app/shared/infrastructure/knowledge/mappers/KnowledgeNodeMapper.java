package com.rag.app.shared.infrastructure.knowledge.mappers;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.valueobjects.ConfidenceScore;
import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeType;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class KnowledgeNodeMapper {
    private static final String NODE_ID = "nodeId";
    private static final String LABEL = "label";
    private static final String NODE_TYPE = "nodeType";
    private static final String GRAPH_ID = "graphId";
    private static final String CONFIDENCE = "confidence";
    private static final String CREATED_AT = "createdAt";
    private static final String LAST_UPDATED_AT = "lastUpdatedAt";
    private static final String SOURCE_DOCUMENT_ID = "sourceDocumentId";
    private static final String SOURCE_DOCUMENT_NAME = "sourceDocumentName";
    private static final String SOURCE_SECTION_REFERENCE = "sourceSectionReference";
    private static final String SOURCE_RELEVANCE_SCORE = "sourceRelevanceScore";

    public KnowledgeNode fromNeo4jNode(Node neo4jNode) {
        Map<String, Object> properties = extractProperties(neo4jNode);
        Map<String, Object> customProperties = new LinkedHashMap<>(properties);
        removeSystemKeys(customProperties);

        return new KnowledgeNode(
            new NodeId((String) properties.get(NODE_ID)),
            (String) properties.get(LABEL),
            NodeType.valueOf((String) properties.get(NODE_TYPE)),
            customProperties,
            sourceDocument(properties),
            new ConfidenceScore(toDouble(properties.get(CONFIDENCE))),
            Instant.parse((String) properties.get(CREATED_AT)),
            Instant.parse((String) properties.get(LAST_UPDATED_AT))
        );
    }

    public Map<String, Object> toNeo4jProperties(KnowledgeNode node, GraphId graphId) {
        Map<String, Object> properties = new LinkedHashMap<>(node.properties());
        properties.put(NODE_ID, node.nodeId().value());
        properties.put(LABEL, node.label());
        properties.put(NODE_TYPE, node.nodeType().name());
        properties.put(GRAPH_ID, graphId.value());
        properties.put(CONFIDENCE, node.confidence().value());
        properties.put(CREATED_AT, node.createdAt().toString());
        properties.put(LAST_UPDATED_AT, node.lastUpdatedAt().toString());
        addSourceDocument(properties, node.sourceDocument());
        return properties;
    }

    private Map<String, Object> extractProperties(Node neo4jNode) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String key : neo4jNode.keys()) {
            Value value = neo4jNode.get(key);
            properties.put(key, value.isNull() ? null : value.asObject());
        }
        return properties;
    }

    private void removeSystemKeys(Map<String, Object> customProperties) {
        customProperties.remove(NODE_ID);
        customProperties.remove(LABEL);
        customProperties.remove(NODE_TYPE);
        customProperties.remove(GRAPH_ID);
        customProperties.remove(CONFIDENCE);
        customProperties.remove(CREATED_AT);
        customProperties.remove(LAST_UPDATED_AT);
        customProperties.remove(SOURCE_DOCUMENT_ID);
        customProperties.remove(SOURCE_DOCUMENT_NAME);
        customProperties.remove(SOURCE_SECTION_REFERENCE);
        customProperties.remove(SOURCE_RELEVANCE_SCORE);
    }

    private DocumentReference sourceDocument(Map<String, Object> properties) {
        Object documentId = properties.get(SOURCE_DOCUMENT_ID);
        Object documentName = properties.get(SOURCE_DOCUMENT_NAME);
        if (documentId == null || documentName == null) {
            return null;
        }
        return new DocumentReference(
            UUID.fromString((String) documentId),
            (String) documentName,
            (String) properties.get(SOURCE_SECTION_REFERENCE),
            toDouble(properties.getOrDefault(SOURCE_RELEVANCE_SCORE, 0.0d))
        );
    }

    private void addSourceDocument(Map<String, Object> properties, DocumentReference sourceDocument) {
        if (sourceDocument == null) {
            return;
        }
        properties.put(SOURCE_DOCUMENT_ID, sourceDocument.documentId().toString());
        properties.put(SOURCE_DOCUMENT_NAME, sourceDocument.documentName());
        properties.put(SOURCE_SECTION_REFERENCE, sourceDocument.sectionReference());
        properties.put(SOURCE_RELEVANCE_SCORE, sourceDocument.relevanceScore());
    }

    private double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value));
    }
}

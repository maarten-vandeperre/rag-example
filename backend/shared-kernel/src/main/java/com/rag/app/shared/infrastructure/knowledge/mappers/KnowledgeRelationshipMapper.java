package com.rag.app.shared.infrastructure.knowledge.mappers;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.valueobjects.ConfidenceScore;
import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipId;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Relationship;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class KnowledgeRelationshipMapper {
    private static final String RELATIONSHIP_ID = "relationshipId";
    private static final String RELATIONSHIP_TYPE = "relationshipType";
    private static final String CONFIDENCE = "confidence";
    private static final String CREATED_AT = "createdAt";
    private static final String SOURCE_DOCUMENT_ID = "sourceDocumentId";
    private static final String SOURCE_DOCUMENT_NAME = "sourceDocumentName";
    private static final String SOURCE_SECTION_REFERENCE = "sourceSectionReference";
    private static final String SOURCE_RELEVANCE_SCORE = "sourceRelevanceScore";

    public KnowledgeRelationship fromNeo4jRelationship(Relationship neo4jRelationship, String fromNodeId, String toNodeId) {
        Map<String, Object> properties = extractProperties(neo4jRelationship);
        Map<String, Object> customProperties = new LinkedHashMap<>(properties);
        customProperties.remove(RELATIONSHIP_ID);
        customProperties.remove(RELATIONSHIP_TYPE);
        customProperties.remove(CONFIDENCE);
        customProperties.remove(CREATED_AT);
        customProperties.remove(SOURCE_DOCUMENT_ID);
        customProperties.remove(SOURCE_DOCUMENT_NAME);
        customProperties.remove(SOURCE_SECTION_REFERENCE);
        customProperties.remove(SOURCE_RELEVANCE_SCORE);

        return new KnowledgeRelationship(
            new RelationshipId((String) properties.get(RELATIONSHIP_ID)),
            new NodeId(fromNodeId),
            new NodeId(toNodeId),
            RelationshipType.valueOf((String) properties.get(RELATIONSHIP_TYPE)),
            customProperties,
            sourceDocument(properties),
            new ConfidenceScore(toDouble(properties.get(CONFIDENCE))),
            Instant.parse((String) properties.get(CREATED_AT))
        );
    }

    public Map<String, Object> toNeo4jProperties(KnowledgeRelationship relationship) {
        Map<String, Object> properties = new LinkedHashMap<>(relationship.properties());
        properties.put(RELATIONSHIP_ID, relationship.relationshipId().value());
        properties.put(RELATIONSHIP_TYPE, relationship.relationshipType().name());
        properties.put(CONFIDENCE, relationship.confidence().value());
        properties.put(CREATED_AT, relationship.createdAt().toString());
        addSourceDocument(properties, relationship.sourceDocument());
        return properties;
    }

    private Map<String, Object> extractProperties(Relationship neo4jRelationship) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String key : neo4jRelationship.keys()) {
            Value value = neo4jRelationship.get(key);
            properties.put(key, value.isNull() ? null : value.asObject());
        }
        return properties;
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

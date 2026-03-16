package com.rag.app.shared.infrastructure.knowledge.mappers;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphMetadata;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class KnowledgeGraphMapper {
    private static final String ATTRIBUTES_PREFIX = "metadataAttribute_";
    private static final String SOURCE_DOCUMENTS = "sourceDocuments";
    private static final String DESCRIPTION = "description";

    public KnowledgeGraph fromNeo4jRecord(Record graphRecord, Set<KnowledgeNode> nodes, Set<KnowledgeRelationship> relationships) {
        var graphNode = graphRecord.get("g").asNode();
        return new KnowledgeGraph(
            new GraphId(graphNode.get("graphId").asString()),
            graphNode.get("name").asString(),
            Set.copyOf(nodes),
            Set.copyOf(relationships),
            graphMetadata(graphNode),
            Instant.parse(graphNode.get("createdAt").asString()),
            Instant.parse(graphNode.get("lastUpdatedAt").asString())
        );
    }

    public Map<String, Object> toNeo4jProperties(KnowledgeGraph graph) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("graphId", graph.graphId().value());
        properties.put("name", graph.name());
        properties.put(DESCRIPTION, graph.metadata().description());
        properties.put("createdAt", graph.createdAt().toString());
        properties.put("lastUpdatedAt", graph.lastUpdatedAt().toString());
        properties.put(SOURCE_DOCUMENTS, serializeDocuments(graph.metadata().sourceDocuments()));
        graph.metadata().attributes().forEach((key, value) -> properties.put(ATTRIBUTES_PREFIX + key, value));
        return properties;
    }

    private GraphMetadata graphMetadata(org.neo4j.driver.types.Node graphNode) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (String key : graphNode.keys()) {
            if (key.startsWith(ATTRIBUTES_PREFIX)) {
                attributes.put(key.substring(ATTRIBUTES_PREFIX.length()), toObject(graphNode.get(key)));
            }
        }
        return new GraphMetadata(
            graphNode.get(DESCRIPTION).isNull() ? "" : graphNode.get(DESCRIPTION).asString(),
            deserializeDocuments(graphNode.get(SOURCE_DOCUMENTS)),
            attributes
        );
    }

    private List<String> serializeDocuments(Set<DocumentReference> sourceDocuments) {
        List<String> serialized = new ArrayList<>();
        for (DocumentReference sourceDocument : sourceDocuments) {
            serialized.add(String.join("|",
                sourceDocument.documentId().toString(),
                escape(sourceDocument.documentName()),
                escape(sourceDocument.sectionReference()),
                Double.toString(sourceDocument.relevanceScore())
            ));
        }
        return List.copyOf(serialized);
    }

    private Set<DocumentReference> deserializeDocuments(Value value) {
        if (value == null || value.isNull()) {
            return Set.of();
        }
        Set<DocumentReference> documents = new LinkedHashSet<>();
        for (Object entry : value.asList(Value::asObject)) {
            String[] parts = String.valueOf(entry).split("\\|", -1);
            documents.add(new DocumentReference(
                UUID.fromString(parts[0]),
                unescape(parts[1]),
                parts[2].isEmpty() ? null : unescape(parts[2]),
                Double.parseDouble(parts[3])
            ));
        }
        return Set.copyOf(documents);
    }

    private Object toObject(Value value) {
        return value.isNull() ? null : value.asObject();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("|", "\\|");
    }

    private String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (char current : value.toCharArray()) {
            if (escaped) {
                result.append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }
}

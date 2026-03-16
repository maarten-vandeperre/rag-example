package com.rag.app.shared.domain.knowledge.valueobjects;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record GraphMetadata(String description,
                            Set<DocumentReference> sourceDocuments,
                            Map<String, Object> attributes) {
    public GraphMetadata {
        description = description == null ? "" : description.trim();
        sourceDocuments = Set.copyOf(Objects.requireNonNull(sourceDocuments, "sourceDocuments cannot be null"));
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes cannot be null"));
    }

    public static GraphMetadata empty() {
        return new GraphMetadata("", Set.of(), Map.of());
    }

    public boolean containsSourceDocument(DocumentReference documentReference) {
        return sourceDocuments.contains(documentReference);
    }
}

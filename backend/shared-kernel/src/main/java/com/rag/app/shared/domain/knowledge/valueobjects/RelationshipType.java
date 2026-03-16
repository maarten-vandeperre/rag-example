package com.rag.app.shared.domain.knowledge.valueobjects;

public enum RelationshipType {
    RELATED_TO("Related To"),
    PART_OF("Part Of"),
    MENTIONS("Mentions"),
    DEFINED_IN("Defined In"),
    SIMILAR_TO("Similar To"),
    DEPENDS_ON("Depends On"),
    CONTAINS("Contains"),
    REFERENCES("References"),
    DERIVED_FROM("Derived From");

    private final String displayName;

    RelationshipType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

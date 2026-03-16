package com.rag.app.shared.domain.knowledge.valueobjects;

public enum NodeType {
    CONCEPT("Concept"),
    ENTITY("Entity"),
    PERSON("Person"),
    ORGANIZATION("Organization"),
    LOCATION("Location"),
    EVENT("Event"),
    DOCUMENT_SECTION("Document Section"),
    TOPIC("Topic"),
    KEYWORD("Keyword");

    private final String displayName;

    NodeType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

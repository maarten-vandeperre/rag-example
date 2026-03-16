package com.rag.app.shared.domain.knowledge.valueobjects;

import com.rag.app.shared.domain.valueobjects.EntityId;

import java.util.UUID;

public final class GraphId extends EntityId {
    public GraphId(String value) {
        super(value);
    }

    public static GraphId generate() {
        return new GraphId(UUID.randomUUID().toString());
    }
}

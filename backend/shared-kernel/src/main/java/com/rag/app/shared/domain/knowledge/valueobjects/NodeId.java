package com.rag.app.shared.domain.knowledge.valueobjects;

import com.rag.app.shared.domain.valueobjects.EntityId;

import java.util.UUID;

public final class NodeId extends EntityId {
    public NodeId(String value) {
        super(value);
    }

    public static NodeId generate() {
        return new NodeId(UUID.randomUUID().toString());
    }
}

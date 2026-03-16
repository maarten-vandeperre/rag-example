package com.rag.app.shared.domain.knowledge.valueobjects;

import com.rag.app.shared.domain.valueobjects.EntityId;

import java.util.UUID;

public final class RelationshipId extends EntityId {
    public RelationshipId(String value) {
        super(value);
    }

    public static RelationshipId generate() {
        return new RelationshipId(UUID.randomUUID().toString());
    }
}

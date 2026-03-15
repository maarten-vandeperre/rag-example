package com.rag.app.user.domain.valueobjects;

import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

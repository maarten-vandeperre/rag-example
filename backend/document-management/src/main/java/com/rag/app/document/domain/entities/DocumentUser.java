package com.rag.app.document.domain.entities;

import com.rag.app.document.domain.valueobjects.UserRole;

import java.util.Objects;

public record DocumentUser(String userId, String username, UserRole role, boolean active) {
    public DocumentUser {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or empty");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be null or empty");
        }
        Objects.requireNonNull(role, "role must not be null");
    }
}

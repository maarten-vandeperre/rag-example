package com.rag.app.user.domain.valueobjects;

public record UserCredentials(String username, String password) {
    public UserCredentials {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be null or empty");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password must not be null or empty");
        }
    }
}

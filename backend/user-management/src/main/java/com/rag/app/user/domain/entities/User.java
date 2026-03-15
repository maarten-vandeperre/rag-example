package com.rag.app.user.domain.entities;

import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public final class User {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserId userId;
    private final String username;
    private final String email;
    private final UserRole role;
    private final Instant createdAt;
    private final boolean active;

    public User(UserId userId,
                String username,
                String email,
                UserRole role,
                Instant createdAt,
                boolean active) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be null or empty");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("email must be a valid email address");
        }
        this.username = username;
        this.email = email;
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.active = active;
    }

    public UserId userId() {
        return userId;
    }

    public String username() {
        return username;
    }

    public String email() {
        return email;
    }

    public UserRole role() {
        return role;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public boolean canAccessAllDocuments() {
        return role == UserRole.ADMIN;
    }

    public boolean canAccessDocument(UserId documentOwnerId) {
        return active && (role == UserRole.ADMIN || userId.equals(documentOwnerId));
    }

    public boolean canPerformAdminActions() {
        return active && role == UserRole.ADMIN;
    }

    public User withRole(UserRole newRole) {
        return new User(userId, username, email, newRole, createdAt, active);
    }
}

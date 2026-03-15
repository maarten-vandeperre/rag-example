package com.rag.app.user.usecases.models;

import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;

import java.time.Instant;

public record GetUserProfileOutput(UserId userId, String username, String email, UserRole role, Instant createdAt, boolean active) {
}

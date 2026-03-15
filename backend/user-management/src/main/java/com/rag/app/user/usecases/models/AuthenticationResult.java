package com.rag.app.user.usecases.models;

import com.rag.app.user.domain.entities.User;

public record AuthenticationResult(boolean authenticated, String sessionToken, User user, String errorMessage) {
}

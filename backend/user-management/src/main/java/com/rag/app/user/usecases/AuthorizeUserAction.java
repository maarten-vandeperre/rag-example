package com.rag.app.user.usecases;

import com.rag.app.user.domain.services.AuthorizationService;
import com.rag.app.user.interfaces.UserRepository;
import com.rag.app.user.domain.valueobjects.UserId;

import java.util.Objects;

public final class AuthorizeUserAction {
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

    public AuthorizeUserAction(UserRepository userRepository, AuthorizationService authorizationService) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    public boolean execute(UserId userId, String resource, String action) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return userRepository.findById(userId)
            .map(user -> authorizationService.canPerformAdminAction(user, action)
                || authorizationService.canAccessDocument(user, resource))
            .orElse(false);
    }
}

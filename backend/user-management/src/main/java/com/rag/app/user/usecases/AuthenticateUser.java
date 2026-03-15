package com.rag.app.user.usecases;

import com.rag.app.user.domain.services.UserDomainService;
import com.rag.app.user.domain.valueobjects.UserCredentials;
import com.rag.app.user.interfaces.AuthenticationProvider;
import com.rag.app.user.interfaces.SessionManager;
import com.rag.app.user.usecases.models.AuthenticationRequest;
import com.rag.app.user.usecases.models.AuthenticationResult;

import java.util.Objects;

public final class AuthenticateUser {
    private final AuthenticationProvider authenticationProvider;
    private final SessionManager sessionManager;
    private final UserDomainService userDomainService;

    public AuthenticateUser(AuthenticationProvider authenticationProvider,
                            SessionManager sessionManager,
                            UserDomainService userDomainService) {
        this.authenticationProvider = Objects.requireNonNull(authenticationProvider, "authenticationProvider must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager must not be null");
        this.userDomainService = Objects.requireNonNull(userDomainService, "userDomainService must not be null");
    }

    public AuthenticationResult execute(AuthenticationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return authenticationProvider.authenticate(new UserCredentials(request.username(), request.password()))
            .map(user -> {
                userDomainService.ensureActive(user);
                return new AuthenticationResult(true, sessionManager.createSession(user), user, null);
            })
            .orElseGet(() -> new AuthenticationResult(false, null, null, "Invalid username or password"));
    }
}

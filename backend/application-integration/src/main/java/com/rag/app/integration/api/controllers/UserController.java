package com.rag.app.integration.api.controllers;

import com.rag.app.integration.api.dto.ApiResponse;
import com.rag.app.integration.orchestration.ApplicationOrchestrator;
import com.rag.app.user.usecases.models.AuthenticationRequest;
import com.rag.app.user.usecases.models.AuthenticationResult;

public final class UserController {
    private final ApplicationOrchestrator orchestrator;

    public UserController(ApplicationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public ApiResponse<AuthenticationResult> authenticate(String username, String password) {
        try {
            return ApiResponse.success(orchestrator.authenticateUser(new AuthenticationRequest(username, password)));
        } catch (RuntimeException exception) {
            return ApiResponse.failure("AUTHENTICATION_FAILED", exception.getMessage());
        }
    }
}

package com.rag.app.integration.api.controllers;

import com.rag.app.chat.usecases.models.QueryDocumentsOutput;
import com.rag.app.integration.api.dto.ApiResponse;
import com.rag.app.integration.api.dto.AnswerSourceDetailsResponse;
import com.rag.app.integration.orchestration.ApplicationOrchestrator;

public final class ChatController {
    private final ApplicationOrchestrator orchestrator;

    public ChatController(ApplicationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public ApiResponse<QueryDocumentsOutput> submitQuery(String userId, String question) {
        try {
            return ApiResponse.success(orchestrator.processQuery(userId, question));
        } catch (RuntimeException exception) {
            return ApiResponse.failure("CHAT_QUERY_FAILED", exception.getMessage());
        }
    }
    
    public ApiResponse<AnswerSourceDetailsResponse> getAnswerSourceDetails(String answerId, String userId) {
        try {
            return ApiResponse.success(orchestrator.getAnswerSourceDetails(answerId, userId));
        } catch (RuntimeException exception) {
            return ApiResponse.failure("ANSWER_SOURCE_DETAILS_FAILED", exception.getMessage());
        }
    }
}

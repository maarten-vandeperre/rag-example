package com.rag.app.chat.usecases.models;

/**
 * Input model for retrieving answer source details.
 */
public record GetAnswerSourceDetailsInput(String answerId, String userId) {
    public GetAnswerSourceDetailsInput {
        if (answerId == null || answerId.isBlank()) {
            throw new IllegalArgumentException("answerId must not be null or blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
    }
}
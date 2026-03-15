package com.rag.app.chat.usecases;

import com.rag.app.chat.interfaces.AnswerGenerator;
import com.rag.app.chat.usecases.models.DocumentChunk;

import java.util.List;
import java.util.Objects;

public final class GenerateAnswer {
    private final AnswerGenerator answerGenerator;

    public GenerateAnswer(AnswerGenerator answerGenerator) {
        this.answerGenerator = Objects.requireNonNull(answerGenerator, "answerGenerator must not be null");
    }

    public AnswerGenerator.GeneratedAnswer execute(String question, List<DocumentChunk> context) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
        return answerGenerator.generateAnswer(question, context);
    }
}

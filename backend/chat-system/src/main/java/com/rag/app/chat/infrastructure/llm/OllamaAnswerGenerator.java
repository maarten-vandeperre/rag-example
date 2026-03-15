package com.rag.app.chat.infrastructure.llm;

import com.rag.app.chat.domain.valueobjects.DocumentReference;
import com.rag.app.chat.interfaces.AnswerGenerator;
import com.rag.app.chat.usecases.models.DocumentChunk;

import java.util.List;

public final class OllamaAnswerGenerator implements AnswerGenerator {
    @Override
    public GeneratedAnswer generateAnswer(String question, List<DocumentChunk> context) {
        if (context == null || context.isEmpty()) {
            return new GeneratedAnswer("No relevant documents found for the question", List.of());
        }
        List<DocumentReference> references = context.stream()
            .map(chunk -> new DocumentReference(chunk.documentId(), chunk.documentName(), chunk.paragraphReference(), chunk.relevanceScore()))
            .toList();
        return new GeneratedAnswer("Generated answer for: " + question, references);
    }
}

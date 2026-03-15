package com.rag.app.infrastructure.llm;

import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.usecases.interfaces.AnswerGenerator;
import com.rag.app.usecases.models.DocumentChunk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class AnswerGeneratorImpl implements AnswerGenerator {
    private final PromptTemplate promptTemplate;
    private final LlmClient llmClient;
    private final ResponseValidator responseValidator;

    @Inject
    public AnswerGeneratorImpl(PromptTemplate promptTemplate,
                               LlmClient llmClient,
                               ResponseValidator responseValidator) {
        this.promptTemplate = Objects.requireNonNull(promptTemplate, "promptTemplate must not be null");
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient must not be null");
        this.responseValidator = Objects.requireNonNull(responseValidator, "responseValidator must not be null");
    }

    @Override
    public GeneratedAnswer generateAnswer(String question, List<DocumentChunk> context) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
        if (context == null || context.isEmpty()) {
            return new GeneratedAnswer(ResponseValidator.NO_INFORMATION_MESSAGE, List.of());
        }

        String prompt = promptTemplate.build(question, context);

        try {
            String answer = llmClient.generate(prompt, question, context);
            List<DocumentReference> references = referencesFor(answer, context);
            String validated = responseValidator.validate(answer, question, context, references);
            if (ResponseValidator.NO_INFORMATION_MESSAGE.equals(validated)) {
                return new GeneratedAnswer(validated, List.of());
            }
            return new GeneratedAnswer(validated, references);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Failed to generate answer", exception);
        }
    }

    private List<DocumentReference> referencesFor(String answer, List<DocumentChunk> context) {
        if (ResponseValidator.NO_INFORMATION_MESSAGE.equals(answer == null ? null : answer.strip())) {
            return List.of();
        }

        String normalizedAnswer = answer.toLowerCase();
        List<DocumentReference> references = context.stream()
            .filter(chunk -> normalizedAnswer.contains(chunk.documentName().toLowerCase())
                || normalizedAnswer.contains(chunk.paragraphReference().toLowerCase())
                || overlap(chunk, normalizedAnswer))
            .sorted(Comparator.comparingDouble(DocumentChunk::relevanceScore).reversed())
            .map(chunk -> new DocumentReference(chunk.documentId(), chunk.documentName(), chunk.paragraphReference(), chunk.relevanceScore()))
            .distinct()
            .toList();

        if (!references.isEmpty()) {
            return references;
        }

        return context.stream()
            .sorted(Comparator.comparingDouble(DocumentChunk::relevanceScore).reversed())
            .limit(Math.min(2, context.size()))
            .map(chunk -> new DocumentReference(chunk.documentId(), chunk.documentName(), chunk.paragraphReference(), chunk.relevanceScore()))
            .toList();
    }

    private boolean overlap(DocumentChunk chunk, String normalizedAnswer) {
        return List.of(chunk.text().toLowerCase().split("\\s+"))
            .stream()
            .filter(token -> token.length() > 4)
            .anyMatch(normalizedAnswer::contains);
    }
}

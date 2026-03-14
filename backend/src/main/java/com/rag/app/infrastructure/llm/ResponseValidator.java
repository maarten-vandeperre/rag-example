package com.rag.app.infrastructure.llm;

import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.usecases.models.DocumentChunk;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ResponseValidator {
    public static final String NO_INFORMATION_MESSAGE = "I cannot find relevant information in the provided documents.";

    public String validate(String answer, String question, List<DocumentChunk> context, List<DocumentReference> references) {
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("answer must not be null or empty");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
        if (context == null || context.isEmpty()) {
            throw new IllegalArgumentException("context must not be null or empty");
        }

        String normalizedAnswer = answer.strip();
        if (NO_INFORMATION_MESSAGE.equals(normalizedAnswer)) {
            return normalizedAnswer;
        }

        if (references == null || references.isEmpty()) {
            throw new IllegalStateException("Generated answer must include source references");
        }

        Set<String> contextTerms = context.stream()
            .flatMap(chunk -> tokenize(chunk.text()).stream())
            .collect(Collectors.toSet());
        Set<String> answerTerms = tokenize(normalizedAnswer);
        answerTerms.removeAll(stopWords());

        long groundedTerms = answerTerms.stream().filter(contextTerms::contains).count();
        if (groundedTerms == 0) {
            throw new IllegalStateException("Generated answer must be grounded in the provided context");
        }

        return normalizedAnswer;
    }

    private Set<String> tokenize(String value) {
        return List.of(value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").trim().split("\\s+"))
            .stream()
            .filter(token -> !token.isBlank())
            .collect(Collectors.toSet());
    }

    private Set<String> stopWords() {
        return Set.of("a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "how", "i", "in", "is",
            "it", "of", "on", "or", "the", "to", "user", "what", "when", "where", "with");
    }
}

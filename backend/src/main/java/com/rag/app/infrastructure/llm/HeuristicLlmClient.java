package com.rag.app.infrastructure.llm;

import com.rag.app.usecases.models.DocumentChunk;

import javax.enterprise.context.ApplicationScoped;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class HeuristicLlmClient implements LlmClient {
    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "and", "are", "as", "at", "be", "by", "do", "does", "for", "from", "how", "in", "is",
        "it", "of", "on", "or", "the", "to", "what", "when", "where", "which", "with"
    );

    @Override
    public String generate(String prompt, String question, List<DocumentChunk> context) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be null or empty");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
        if (context == null || context.isEmpty()) {
            throw new IllegalArgumentException("context must not be null or empty");
        }

        Set<String> questionTerms = tokenize(question);
        questionTerms.removeAll(STOP_WORDS);
        List<DocumentChunk> rankedChunks = context.stream()
            .sorted(Comparator.<DocumentChunk>comparingLong(chunk -> overlap(questionTerms, tokenize(chunk.text()))).reversed()
                .thenComparing(Comparator.comparingDouble(DocumentChunk::relevanceScore).reversed()))
            .toList();

        long bestOverlap = overlap(questionTerms, tokenize(rankedChunks.get(0).text()));
        if (bestOverlap == 0) {
            return ResponseValidator.NO_INFORMATION_MESSAGE;
        }

        return rankedChunks.stream()
            .filter(chunk -> overlap(questionTerms, tokenize(chunk.text())) > 0)
            .limit(2)
            .map(chunk -> summarize(chunk.text()) + " (Source: " + chunk.documentName() + " " + chunk.paragraphReference() + ")")
            .collect(Collectors.joining(" "));
    }

    private long overlap(Set<String> left, Set<String> right) {
        return left.stream().filter(right::contains).count();
    }

    private Set<String> tokenize(String value) {
        return List.of(value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").trim().split("\\s+"))
            .stream()
            .filter(token -> !token.isBlank())
            .collect(Collectors.toSet());
    }

    private String summarize(String text) {
        String trimmed = text.strip();
        int sentenceEnd = trimmed.indexOf('.');
        if (sentenceEnd > 0) {
            return trimmed.substring(0, sentenceEnd + 1);
        }
        return trimmed;
    }
}

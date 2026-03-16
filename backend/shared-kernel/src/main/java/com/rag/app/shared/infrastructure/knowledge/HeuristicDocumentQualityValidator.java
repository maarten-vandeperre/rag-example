package com.rag.app.shared.infrastructure.knowledge;

import com.rag.app.shared.interfaces.knowledge.DocumentQualityResult;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HeuristicDocumentQualityValidator implements DocumentQualityValidator {
    private static final int MINIMUM_CONTENT_LENGTH = 80;

    @Override
    public DocumentQualityResult validateForKnowledgeExtraction(String content, String documentType) {
        List<String> warnings = new ArrayList<>();
        List<String> issues = new ArrayList<>();

        if (!hasMinimumContentLength(content)) {
            issues.add("Document content is too short for reliable knowledge extraction");
        }
        if (!hasAcceptableLanguage(content)) {
            issues.add("Document content does not appear to be predominantly English-like text");
        }
        if (!hasStructuredContent(content, documentType)) {
            warnings.add("Document structure is limited, so extracted knowledge may be incomplete");
        }

        if (content != null && content.trim().length() < 200) {
            warnings.add("Short documents may produce only a small knowledge graph");
        }
        if (content != null && content.chars().filter(Character::isDigit).count() > Math.max(12, content.length() / 5)) {
            warnings.add("Document appears data-heavy, which may reduce extraction accuracy");
        }

        return issues.isEmpty()
            ? DocumentQualityResult.sufficient(warnings)
            : new DocumentQualityResult(false, warnings, issues);
    }

    @Override
    public boolean hasMinimumContentLength(String content) {
        return content != null && content.trim().length() >= MINIMUM_CONTENT_LENGTH;
    }

    @Override
    public boolean hasAcceptableLanguage(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        long acceptedCharacters = content.chars()
            .filter(character -> Character.isLetter(character)
                || Character.isWhitespace(character)
                || ".,;:!?-'#/()[]".indexOf(character) >= 0)
            .count();
        double ratio = acceptedCharacters / (double) content.length();
        return ratio >= 0.70d;
    }

    @Override
    public boolean hasStructuredContent(String content, String documentType) {
        if (content == null || content.isBlank()) {
            return false;
        }

        String normalizedType = documentType == null ? "" : documentType.trim().toUpperCase(Locale.ROOT);
        long sentenceCount = content.split("(?<=[.!?])\\s+").length;
        long paragraphCount = content.split("\\R\\s*\\R").length;

        if ("MARKDOWN".equals(normalizedType)) {
            return content.contains("#") || sentenceCount >= 2 || paragraphCount >= 2;
        }

        return sentenceCount >= 2 || paragraphCount >= 2;
    }
}

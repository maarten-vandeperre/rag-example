package com.rag.app.shared.interfaces.knowledge;

public interface DocumentQualityValidator {
    DocumentQualityResult validateForKnowledgeExtraction(String content, String documentType);

    boolean hasMinimumContentLength(String content);

    boolean hasAcceptableLanguage(String content);

    boolean hasStructuredContent(String content, String documentType);
}

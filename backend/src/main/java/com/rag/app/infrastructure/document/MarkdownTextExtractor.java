package com.rag.app.infrastructure.document;

public final class MarkdownTextExtractor {

    public String extract(String markdown) {
        String text = markdown
            .replaceAll("```[\\s\\S]*?```", " ")
            .replaceAll("`([^`]*)`", "$1")
            .replaceAll("!\\[[^\\]]*]\\(([^)]+)\\)", " ")
            .replaceAll("\\[([^\\]]+)]\\(([^)]+)\\)", "$1")
            .replaceAll("(?m)^#{1,6}\\s*", "")
            .replaceAll("(?m)^>\\s?", "")
            .replaceAll("(?m)^[-*+]\\s+", "")
            .replaceAll("(?m)^\\d+\\.\\s+", "")
            .replaceAll("[*_~]{1,3}", "")
            .replaceAll("(?m)^---+$", " ")
            .replaceAll("\\r", "")
            .replaceAll("\\n{3,}", "\\n\\n");

        return text.strip();
    }
}

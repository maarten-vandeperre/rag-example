package com.rag.app.infrastructure.llm;

import com.rag.app.usecases.models.DocumentChunk;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class PromptTemplate {
    private static final String TEMPLATE = """
        Based on the following document excerpts, answer the user's question.
        If the answer cannot be found in the provided context, say \"I cannot find relevant information in the provided documents.\"

        Context:
        %s

        Question: %s

        Instructions:
        - Provide a clear, concise answer based only on the provided context
        - Include specific references to source documents and sections
        - If information is unclear or missing, acknowledge this limitation
        - Do not make assumptions beyond what is stated in the context

        Answer:
        """;

    public String build(String question, List<DocumentChunk> context) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
        if (context == null || context.isEmpty()) {
            throw new IllegalArgumentException("context must not be null or empty");
        }

        String renderedContext = context.stream()
            .map(chunk -> "- Source: " + chunk.documentName() + " (" + chunk.paragraphReference() + ")\n"
                + "  Excerpt: " + chunk.text())
            .reduce((left, right) -> left + "\n" + right)
            .orElseThrow();

        return TEMPLATE.formatted(renderedContext, question.strip());
    }
}

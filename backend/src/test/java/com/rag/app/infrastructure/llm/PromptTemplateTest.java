package com.rag.app.infrastructure.llm;

import com.rag.app.usecases.models.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptTemplateTest {

    @Test
    void shouldRenderQuestionAndContextWithSourceMetadata() {
        PromptTemplate promptTemplate = new PromptTemplate();

        String prompt = promptTemplate.build("How do uploads work?", List.of(
            new DocumentChunk("chunk-1", UUID.randomUUID(), "guide.pdf", 0, "chunk-1", "Uploads are processed asynchronously.", new double[0], 0.94d)
        ));

        assertTrue(prompt.contains("Question: How do uploads work?"));
        assertTrue(prompt.contains("Source: guide.pdf (chunk-1)"));
        assertTrue(prompt.contains("Uploads are processed asynchronously."));
    }
}

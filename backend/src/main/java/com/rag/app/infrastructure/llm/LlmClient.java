package com.rag.app.infrastructure.llm;

import com.rag.app.usecases.models.DocumentChunk;

import java.util.List;

public interface LlmClient {
    String generate(String prompt, String question, List<DocumentChunk> context);
}

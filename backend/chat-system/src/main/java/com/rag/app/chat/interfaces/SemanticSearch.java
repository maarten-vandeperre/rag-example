package com.rag.app.chat.interfaces;

import com.rag.app.chat.usecases.models.DocumentChunk;

import java.util.List;

public interface SemanticSearch {
    List<DocumentChunk> searchDocuments(String query, List<String> documentIds);
}

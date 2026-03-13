package com.rag.app.usecases.interfaces;

import com.rag.app.usecases.models.DocumentChunk;

import java.util.List;

public interface SemanticSearch {
    List<DocumentChunk> searchDocuments(String query, List<String> documentIds);
}

package com.rag.app.chat.infrastructure.search;

import com.rag.app.chat.interfaces.SemanticSearch;
import com.rag.app.chat.interfaces.VectorStore;
import com.rag.app.chat.usecases.models.DocumentChunk;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WeaviateVectorStore implements VectorStore, SemanticSearch {
    private final Map<String, String> documentText = new ConcurrentHashMap<>();

    @Override
    public void storeDocumentVectors(String documentId, String text) {
        documentText.put(documentId, text);
    }

    @Override
    public void removeDocumentVectors(String documentId) {
        documentText.remove(documentId);
    }

    @Override
    public List<DocumentChunk> searchDocuments(String query, List<String> documentIds) {
        return documentIds.stream()
            .filter(documentText::containsKey)
            .filter(documentId -> documentText.get(documentId).toLowerCase().contains(query.toLowerCase())
                || query.toLowerCase().contains(documentText.get(documentId).toLowerCase()))
            .map(documentId -> new DocumentChunk(java.util.UUID.fromString(documentId), "document-" + documentId, "paragraph-1", documentText.get(documentId), 0.9d))
            .toList();
    }
}

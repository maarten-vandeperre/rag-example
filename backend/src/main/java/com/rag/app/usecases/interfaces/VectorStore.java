package com.rag.app.usecases.interfaces;

public interface VectorStore {
    void storeDocumentVectors(String documentId, String text);
}

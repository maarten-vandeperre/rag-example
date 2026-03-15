package com.rag.app.chat.interfaces;

public interface VectorStore {
    void storeDocumentVectors(String documentId, String text);

    void removeDocumentVectors(String documentId);
}

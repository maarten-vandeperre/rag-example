package com.rag.app.document.interfaces;

import java.util.UUID;

public interface DocumentVectorStore {
    void storeDocument(UUID documentId, String text);
}

package com.rag.app.document.interfaces;

import java.util.Optional;
import java.util.UUID;

public interface DocumentStorage {
    void store(UUID documentId, byte[] fileContent);

    Optional<byte[]> load(UUID documentId);
}

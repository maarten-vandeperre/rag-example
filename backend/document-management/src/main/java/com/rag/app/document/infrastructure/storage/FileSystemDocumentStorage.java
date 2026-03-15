package com.rag.app.document.infrastructure.storage;

import com.rag.app.document.interfaces.DocumentStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class FileSystemDocumentStorage implements DocumentStorage {
    private final Path storageRoot;

    public FileSystemDocumentStorage(Path storageRoot) {
        this.storageRoot = Objects.requireNonNull(storageRoot, "storageRoot must not be null");
    }

    @Override
    public void store(UUID documentId, byte[] fileContent) {
        try {
            Files.createDirectories(storageRoot);
            Files.write(storageRoot.resolve(documentId + ".bin"), fileContent);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store document", exception);
        }
    }

    @Override
    public Optional<byte[]> load(UUID documentId) {
        Path file = storageRoot.resolve(documentId + ".bin");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read document", exception);
        }
    }
}

package com.rag.app.domain.valueobjects;

import java.util.Objects;

public record DocumentMetadata(String fileName, long fileSize, FileType fileType, String contentHash) {
    public static final long MAX_FILE_SIZE_BYTES = 41_943_040L;

    public DocumentMetadata {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be null or empty");
        }
        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("fileSize must be positive and less than or equal to 41943040");
        }

        Objects.requireNonNull(fileType, "fileType must not be null");

        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException("contentHash must not be null or empty");
        }
    }
}

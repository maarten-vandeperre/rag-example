package com.rag.app.shared.domain.valueobjects;

import com.rag.app.shared.domain.exceptions.ValidationException;

public final class FileSize {
    public static final long MAX_FILE_SIZE_BYTES = 41_943_040L;

    private final long bytes;

    public FileSize(long bytes) {
        if (bytes < 0) {
            throw new ValidationException("File size cannot be negative");
        }
        if (bytes > MAX_FILE_SIZE_BYTES) {
            throw new ValidationException("File size exceeds maximum allowed size of 40MB");
        }
        this.bytes = bytes;
    }

    public long bytes() {
        return bytes;
    }

    public double megabytes() {
        return bytes / 1_048_576.0;
    }

    public boolean isWithinLimit() {
        return bytes <= MAX_FILE_SIZE_BYTES;
    }
}

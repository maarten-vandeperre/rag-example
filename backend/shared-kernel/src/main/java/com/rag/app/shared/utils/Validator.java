package com.rag.app.shared.utils;

import com.rag.app.shared.domain.exceptions.ValidationException;

public final class Validator {
    private Validator() {
    }

    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be null or empty");
        }
    }

    public static void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
    }
}

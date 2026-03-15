package com.rag.app.shared.domain.exceptions;

public final class ValidationException extends DomainException {
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}

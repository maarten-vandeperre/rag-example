package com.rag.app.shared.domain.exceptions;

/**
 * Exception thrown when repository operations fail.
 * This is a runtime exception that indicates a problem with data persistence.
 */
public class RepositoryException extends RuntimeException {
    
    public RepositoryException(String message) {
        super(message);
    }
    
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RepositoryException(Throwable cause) {
        super(cause);
    }
}
package com.rag.app.shared.domain.knowledge.valueobjects;

import com.rag.app.shared.domain.exceptions.ValidationException;

public record ConfidenceScore(double value) {
    public ConfidenceScore {
        if (value < 0.0d || value > 1.0d) {
            throw new ValidationException("Confidence score must be between 0.0 and 1.0");
        }
    }

    public static ConfidenceScore high() {
        return new ConfidenceScore(0.9d);
    }

    public static ConfidenceScore medium() {
        return new ConfidenceScore(0.7d);
    }

    public static ConfidenceScore low() {
        return new ConfidenceScore(0.5d);
    }

    public static ConfidenceScore zero() {
        return new ConfidenceScore(0.0d);
    }

    public boolean isHighConfidence() {
        return value >= 0.8d;
    }

    public boolean isMediumConfidence() {
        return value >= 0.6d && value < 0.8d;
    }

    public boolean isLowConfidence() {
        return value < 0.6d;
    }

    public ConfidenceScore mergeWith(ConfidenceScore other) {
        return new ConfidenceScore(Math.max(value, other.value));
    }
}

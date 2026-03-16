package com.rag.app.shared.domain.knowledge.valueobjects;

import com.rag.app.shared.domain.exceptions.ValidationException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ExtractionMetadata(String extractionMethod,
                                 Instant extractedAt,
                                 Duration processingTime,
                                 Map<String, Object> algorithmParameters,
                                 List<String> warnings) {
    public ExtractionMetadata {
        if (extractionMethod == null || extractionMethod.isBlank()) {
            throw new ValidationException("extractionMethod cannot be null or blank");
        }
        Objects.requireNonNull(extractedAt, "extractedAt cannot be null");
        Objects.requireNonNull(processingTime, "processingTime cannot be null");
        if (processingTime.isNegative()) {
            throw new ValidationException("processingTime cannot be negative");
        }
        algorithmParameters = Map.copyOf(Objects.requireNonNull(algorithmParameters, "algorithmParameters cannot be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings cannot be null"));
        extractionMethod = extractionMethod.trim();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}

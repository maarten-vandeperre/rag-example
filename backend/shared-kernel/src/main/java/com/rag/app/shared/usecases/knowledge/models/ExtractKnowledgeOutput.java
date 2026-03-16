package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record ExtractKnowledgeOutput(ExtractedKnowledge extractedKnowledge,
                                     KnowledgeExtractionStatus status,
                                     List<String> warnings,
                                     List<String> errors,
                                     Duration processingTime) {
    public ExtractKnowledgeOutput {
        Objects.requireNonNull(extractedKnowledge, "extractedKnowledge cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings cannot be null"));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors cannot be null"));
        Objects.requireNonNull(processingTime, "processingTime cannot be null");
    }

    public boolean isSuccessful() {
        return status == KnowledgeExtractionStatus.SUCCESS || status == KnowledgeExtractionStatus.PARTIAL_SUCCESS;
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}

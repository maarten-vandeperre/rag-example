package com.rag.app.document.usecases.models;

import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;

import java.util.List;
import java.util.Objects;

public record KnowledgeProcessingResult(boolean successful,
                                        boolean skipped,
                                        boolean disabled,
                                        GraphId graphId,
                                        String errorMessage,
                                        List<String> errors,
                                        List<String> warnings) {
    public KnowledgeProcessingResult {
        errors = List.copyOf(Objects.requireNonNull(errors, "errors cannot be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings cannot be null"));
    }

    public static KnowledgeProcessingResult success(GraphId graphId, List<String> warnings) {
        return new KnowledgeProcessingResult(true, false, false, graphId, null, List.of(), warnings == null ? List.of() : warnings);
    }

    public static KnowledgeProcessingResult failure(String errorMessage, List<String> errors, List<String> warnings) {
        return new KnowledgeProcessingResult(false, false, false, null, errorMessage, errors == null ? List.of() : errors, warnings == null ? List.of() : warnings);
    }

    public static KnowledgeProcessingResult skipped(String reason, List<String> warnings) {
        return new KnowledgeProcessingResult(false, true, false, null, reason, List.of(), warnings == null ? List.of() : warnings);
    }

    public static KnowledgeProcessingResult disabled(String reason) {
        return new KnowledgeProcessingResult(false, false, true, null, reason, List.of(), List.of(reason));
    }

    public boolean isSuccessful() {
        return successful;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public boolean isDisabled() {
        return disabled;
    }
}

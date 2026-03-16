package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.interfaces.knowledge.DocumentQualityResult;

import java.util.List;
import java.util.Objects;

public record ValidateKnowledgeQualityOutput(boolean sufficientForExtraction,
                                             List<String> warnings,
                                             List<String> issues) {
    public ValidateKnowledgeQualityOutput {
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings cannot be null"));
        issues = List.copyOf(Objects.requireNonNull(issues, "issues cannot be null"));
    }

    public static ValidateKnowledgeQualityOutput from(DocumentQualityResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        return new ValidateKnowledgeQualityOutput(result.sufficientForExtraction(), result.warnings(), result.issues());
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }
}

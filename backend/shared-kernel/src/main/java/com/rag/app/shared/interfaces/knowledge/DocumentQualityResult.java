package com.rag.app.shared.interfaces.knowledge;

import java.util.List;
import java.util.Objects;

public record DocumentQualityResult(boolean sufficientForExtraction,
                                    List<String> warnings,
                                    List<String> issues) {
    public DocumentQualityResult {
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings cannot be null"));
        issues = List.copyOf(Objects.requireNonNull(issues, "issues cannot be null"));
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    public static DocumentQualityResult sufficient(List<String> warnings) {
        return new DocumentQualityResult(true, warnings, List.of());
    }

    public static DocumentQualityResult insufficient(List<String> issues) {
        return new DocumentQualityResult(false, List.of(), issues);
    }
}

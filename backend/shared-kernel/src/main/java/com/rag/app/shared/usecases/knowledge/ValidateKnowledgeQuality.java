package com.rag.app.shared.usecases.knowledge;

import com.rag.app.shared.interfaces.UseCase;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityValidator;
import com.rag.app.shared.usecases.knowledge.models.ValidateKnowledgeQualityInput;
import com.rag.app.shared.usecases.knowledge.models.ValidateKnowledgeQualityOutput;

import java.util.Objects;

public final class ValidateKnowledgeQuality implements UseCase<ValidateKnowledgeQualityInput, ValidateKnowledgeQualityOutput> {
    private final DocumentQualityValidator documentQualityValidator;

    public ValidateKnowledgeQuality(DocumentQualityValidator documentQualityValidator) {
        this.documentQualityValidator = Objects.requireNonNull(documentQualityValidator, "documentQualityValidator cannot be null");
    }

    @Override
    public ValidateKnowledgeQualityOutput execute(ValidateKnowledgeQualityInput input) {
        Objects.requireNonNull(input, "input cannot be null");
        return ValidateKnowledgeQualityOutput.from(
            documentQualityValidator.validateForKnowledgeExtraction(input.documentContent(), input.documentType())
        );
    }
}

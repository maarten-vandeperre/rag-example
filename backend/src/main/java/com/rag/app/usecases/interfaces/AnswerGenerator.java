package com.rag.app.usecases.interfaces;

import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.usecases.models.DocumentChunk;

import java.util.List;

public interface AnswerGenerator {
    GeneratedAnswer generateAnswer(String question, List<DocumentChunk> context);

    record GeneratedAnswer(String answer, List<DocumentReference> documentReferences) {
    }
}

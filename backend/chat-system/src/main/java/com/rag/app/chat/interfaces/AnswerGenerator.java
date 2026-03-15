package com.rag.app.chat.interfaces;

import com.rag.app.chat.domain.valueobjects.DocumentReference;
import com.rag.app.chat.usecases.models.DocumentChunk;

import java.util.List;

public interface AnswerGenerator {
    GeneratedAnswer generateAnswer(String question, List<DocumentChunk> context);

    record GeneratedAnswer(String answer, List<DocumentReference> documentReferences) {
    }
}

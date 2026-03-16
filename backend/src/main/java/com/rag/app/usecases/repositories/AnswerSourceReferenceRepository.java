package com.rag.app.usecases.repositories;

import com.rag.app.domain.valueobjects.AnswerSourceReference;

import java.util.List;
import java.util.UUID;

public interface AnswerSourceReferenceRepository {
    void replaceForAnswer(UUID answerId, List<AnswerSourceReference> references);

    List<AnswerSourceReference> findByAnswerId(UUID answerId);
}

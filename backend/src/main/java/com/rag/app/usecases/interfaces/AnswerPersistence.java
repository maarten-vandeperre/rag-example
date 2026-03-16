package com.rag.app.usecases.interfaces;

import com.rag.app.domain.entities.ChatMessage;
import com.rag.app.domain.valueobjects.AnswerSourceReference;

import java.util.List;

public interface AnswerPersistence {
    void persist(ChatMessage message, List<AnswerSourceReference> sourceReferences);
}

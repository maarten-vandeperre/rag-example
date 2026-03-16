package com.rag.app.usecases.interfaces;

import com.rag.app.usecases.models.DocumentChunk;

import java.util.List;
import java.util.UUID;

public interface AnswerSourceChunkStore {
    void store(UUID answerId, List<DocumentChunk> chunks);

    List<DocumentChunk> getChunks(UUID answerId);
}

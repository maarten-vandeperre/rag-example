package com.rag.app.infrastructure.vector;

import com.rag.app.usecases.interfaces.AnswerSourceChunkStore;
import com.rag.app.usecases.models.DocumentChunk;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryAnswerSourceChunkStore implements AnswerSourceChunkStore {
    private final Map<UUID, List<DocumentChunk>> chunksByAnswerId;

    public InMemoryAnswerSourceChunkStore() {
        this(new ConcurrentHashMap<>());
    }

    InMemoryAnswerSourceChunkStore(Map<UUID, List<DocumentChunk>> chunksByAnswerId) {
        this.chunksByAnswerId = Objects.requireNonNull(chunksByAnswerId, "chunksByAnswerId must not be null");
    }

    @Override
    public void store(UUID answerId, List<DocumentChunk> chunks) {
        Objects.requireNonNull(answerId, "answerId must not be null");
        chunksByAnswerId.put(answerId, chunks == null ? List.of() : List.copyOf(chunks));
    }

    @Override
    public List<DocumentChunk> getChunks(UUID answerId) {
        Objects.requireNonNull(answerId, "answerId must not be null");
        return List.copyOf(chunksByAnswerId.getOrDefault(answerId, List.of()));
    }
}

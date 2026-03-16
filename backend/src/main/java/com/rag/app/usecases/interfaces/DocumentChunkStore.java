package com.rag.app.usecases.interfaces;

import com.rag.app.usecases.models.DocumentChunk;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkStore {
    List<DocumentChunk> getDocumentChunks(UUID documentId);
}

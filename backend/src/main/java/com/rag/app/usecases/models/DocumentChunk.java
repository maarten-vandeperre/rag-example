package com.rag.app.usecases.models;

import java.util.UUID;

public record DocumentChunk(UUID documentId,
                            String documentName,
                            String paragraphReference,
                            String text,
                            double relevanceScore) {
}

package com.rag.app.usecases.models;

import java.time.Instant;

public record AnswerSourceMetadata(String title,
                                   String author,
                                   Instant createdAt,
                                   Integer pageNumber,
                                   Integer chunkIndex) {
}

package com.rag.app.api.dto;

import java.time.Instant;

public record AnswerSourceMetadataDto(String title,
                                      String author,
                                      Instant createdAt,
                                      Integer pageNumber,
                                      Integer chunkIndex) {
}

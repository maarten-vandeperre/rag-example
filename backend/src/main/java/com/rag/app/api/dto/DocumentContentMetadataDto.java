package com.rag.app.api.dto;

import java.time.Instant;

public record DocumentContentMetadataDto(String title,
                                         String author,
                                         Instant createdAt,
                                         long fileSize,
                                         int pageCount) {
}

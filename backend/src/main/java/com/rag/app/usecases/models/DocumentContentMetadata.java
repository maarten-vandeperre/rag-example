package com.rag.app.usecases.models;

import java.time.Instant;

public record DocumentContentMetadata(String title,
                                      String author,
                                      Instant createdAt,
                                      long fileSize,
                                      int pageCount) {
}

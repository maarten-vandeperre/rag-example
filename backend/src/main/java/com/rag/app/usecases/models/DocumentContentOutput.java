package com.rag.app.usecases.models;

import java.util.UUID;

public record DocumentContentOutput(UUID documentId,
                                    String fileName,
                                    String fileType,
                                    String content,
                                    DocumentContentMetadata metadata,
                                    boolean available) {
}

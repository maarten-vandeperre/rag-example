package com.rag.app.api.dto;

public record DocumentContentResponse(String documentId,
                                      String fileName,
                                      String fileType,
                                      String content,
                                      DocumentContentMetadataDto metadata,
                                      boolean available) {
}

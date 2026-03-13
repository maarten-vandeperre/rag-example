package com.rag.app.api.dto;

import java.util.List;

public record AdminProgressResponse(ProcessingStatisticsDto statistics,
                                    List<FailedDocumentDto> failedDocuments,
                                    List<ProcessingDocumentDto> processingDocuments) {
}

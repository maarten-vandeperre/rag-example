package com.rag.app.usecases.models;

import java.util.List;

public record GetAdminProgressOutput(ProcessingStatistics processingStatistics,
                                     List<FailedDocumentInfo> failedDocuments,
                                     List<ProcessingDocumentInfo> processingDocuments) {
    public GetAdminProgressOutput {
        failedDocuments = List.copyOf(failedDocuments);
        processingDocuments = List.copyOf(processingDocuments);
    }
}

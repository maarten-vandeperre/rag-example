package com.rag.app.document.usecases.models;

import java.util.List;

public record GetAdminProgressOutput(ProcessingStatistics processingStatistics,
                                     List<FailedDocumentInfo> failedDocuments,
                                     List<ProcessingDocumentInfo> processingDocuments) {
}

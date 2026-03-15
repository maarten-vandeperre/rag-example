package com.rag.app.document.interfaces;

import com.rag.app.document.domain.entities.Document;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.usecases.models.FailedDocumentInfo;
import com.rag.app.document.usecases.models.ProcessingDocumentInfo;
import com.rag.app.document.usecases.models.ProcessingStatistics;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {
    Document save(Document document);

    Optional<Document> findByContentHash(String hash);

    Optional<Document> findById(UUID documentId);

    List<Document> findByUploadedBy(String userId);

    List<Document> findAll();

    List<Document> findByStatus(DocumentStatus status);

    ProcessingStatistics getProcessingStatistics();

    List<FailedDocumentInfo> findFailedDocuments();

    List<ProcessingDocumentInfo> findProcessingDocuments();
}

package com.rag.app.document.usecases;

import com.rag.app.document.domain.entities.DocumentUser;
import com.rag.app.document.domain.services.DocumentDomainService;
import com.rag.app.document.interfaces.DocumentRepository;
import com.rag.app.document.interfaces.DocumentUserDirectory;
import com.rag.app.document.usecases.models.GetAdminProgressInput;
import com.rag.app.document.usecases.models.GetAdminProgressOutput;

import java.util.Comparator;
import java.util.Objects;

public final class GetAdminProgress {
    private final DocumentRepository documentRepository;
    private final DocumentUserDirectory documentUserDirectory;
    private final DocumentDomainService documentDomainService;

    public GetAdminProgress(DocumentRepository documentRepository,
                            DocumentUserDirectory documentUserDirectory,
                            DocumentDomainService documentDomainService) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.documentUserDirectory = Objects.requireNonNull(documentUserDirectory, "documentUserDirectory must not be null");
        this.documentDomainService = Objects.requireNonNull(documentDomainService, "documentDomainService must not be null");
    }

    public GetAdminProgressOutput execute(GetAdminProgressInput input) {
        Objects.requireNonNull(input, "input must not be null");
        if (input.adminUserId() == null || input.adminUserId().isBlank()) {
            throw new IllegalArgumentException("adminUserId must not be null or empty");
        }

        DocumentUser user = documentUserDirectory.findById(input.adminUserId())
            .orElseThrow(() -> new IllegalArgumentException("adminUserId user must exist"));
        documentDomainService.ensureAdmin(user);

        return new GetAdminProgressOutput(
            documentRepository.getProcessingStatistics(),
            documentRepository.findFailedDocuments().stream()
                .sorted(Comparator.comparing(com.rag.app.document.usecases.models.FailedDocumentInfo::uploadedAt).reversed())
                .toList(),
            documentRepository.findProcessingDocuments().stream()
                .sorted(Comparator.comparing(com.rag.app.document.usecases.models.ProcessingDocumentInfo::processingStartedAt))
                .toList()
        );
    }
}

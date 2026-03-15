package com.rag.app.document.usecases;

import com.rag.app.document.domain.entities.Document;
import com.rag.app.document.domain.entities.DocumentUser;
import com.rag.app.document.domain.services.DocumentDomainService;
import com.rag.app.document.interfaces.DocumentRepository;
import com.rag.app.document.interfaces.DocumentUserDirectory;
import com.rag.app.document.usecases.models.DocumentSummary;
import com.rag.app.document.usecases.models.GetUserDocumentsInput;
import com.rag.app.document.usecases.models.GetUserDocumentsOutput;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class GetUserDocuments {
    private final DocumentRepository documentRepository;
    private final DocumentUserDirectory documentUserDirectory;
    private final DocumentDomainService documentDomainService;

    public GetUserDocuments(DocumentRepository documentRepository,
                            DocumentUserDirectory documentUserDirectory,
                            DocumentDomainService documentDomainService) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.documentUserDirectory = Objects.requireNonNull(documentUserDirectory, "documentUserDirectory must not be null");
        this.documentDomainService = Objects.requireNonNull(documentDomainService, "documentDomainService must not be null");
    }

    public GetUserDocumentsOutput execute(GetUserDocumentsInput input) {
        Objects.requireNonNull(input, "input must not be null");
        if (input.userId() == null || input.userId().isBlank()) {
            throw new IllegalArgumentException("userId must not be null or empty");
        }

        DocumentUser user = documentUserDirectory.findById(input.userId())
            .orElseThrow(() -> new IllegalArgumentException("userId user must exist"));

        List<DocumentSummary> documents = resolveDocuments(user, input.includeAllDocuments()).stream()
            .sorted(Comparator.comparing(Document::uploadedAt).reversed())
            .map(document -> new DocumentSummary(
                document.documentId(),
                document.fileName(),
                document.fileSize(),
                document.fileType(),
                document.status(),
                document.uploadedBy(),
                document.uploadedAt(),
                document.lastUpdated()
            ))
            .toList();

        return new GetUserDocumentsOutput(documents, documents.size());
    }

    private List<Document> resolveDocuments(DocumentUser user, boolean includeAllDocuments) {
        if (documentDomainService.canViewAllDocuments(user, includeAllDocuments)) {
            return documentRepository.findAll();
        }
        return documentRepository.findByUploadedBy(user.userId());
    }
}

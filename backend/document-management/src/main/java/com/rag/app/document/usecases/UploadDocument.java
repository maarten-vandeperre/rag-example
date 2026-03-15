package com.rag.app.document.usecases;

import com.rag.app.document.domain.entities.Document;
import com.rag.app.document.domain.entities.DocumentUser;
import com.rag.app.document.domain.services.DocumentDomainService;
import com.rag.app.document.domain.valueobjects.DocumentMetadata;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.interfaces.DocumentRepository;
import com.rag.app.document.interfaces.DocumentStorage;
import com.rag.app.document.interfaces.DocumentUserDirectory;
import com.rag.app.document.usecases.models.UploadDocumentInput;
import com.rag.app.document.usecases.models.UploadDocumentOutput;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

public final class UploadDocument {
    private final DocumentRepository documentRepository;
    private final DocumentUserDirectory documentUserDirectory;
    private final DocumentStorage documentStorage;
    private final DocumentDomainService documentDomainService;
    private final Clock clock;

    public UploadDocument(DocumentRepository documentRepository,
                          DocumentUserDirectory documentUserDirectory,
                          DocumentStorage documentStorage,
                          DocumentDomainService documentDomainService,
                          Clock clock) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.documentUserDirectory = Objects.requireNonNull(documentUserDirectory, "documentUserDirectory must not be null");
        this.documentStorage = Objects.requireNonNull(documentStorage, "documentStorage must not be null");
        this.documentDomainService = Objects.requireNonNull(documentDomainService, "documentDomainService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public UploadDocumentOutput execute(UploadDocumentInput input) {
        Objects.requireNonNull(input, "input must not be null");
        if (input.fileName() == null || input.fileName().isBlank()) {
            throw new IllegalArgumentException("fileName must not be null or empty");
        }
        if (input.fileType() == null) {
            throw new IllegalArgumentException("fileType must not be null");
        }
        if (input.fileContent() == null || input.fileContent().length == 0) {
            throw new IllegalArgumentException("fileContent must not be null or empty");
        }
        if (input.uploadedBy() == null || input.uploadedBy().isBlank()) {
            throw new IllegalArgumentException("uploadedBy must not be null or empty");
        }

        DocumentUser user = documentUserDirectory.findById(input.uploadedBy())
            .orElseThrow(() -> new IllegalArgumentException("uploadedBy user must exist"));
        documentDomainService.validateUploadRequest(user, input.fileSize());

        String contentHash = calculateContentHash(input.fileContent());
        if (documentRepository.findByContentHash(contentHash).isPresent()) {
            throw new IllegalArgumentException("document with identical content already exists");
        }

        Instant now = Instant.now(clock);
        Document document = new Document(
            UUID.randomUUID(),
            new DocumentMetadata(input.fileName(), input.fileSize(), input.fileType(), contentHash),
            user.userId(),
            now,
            now,
            DocumentStatus.UPLOADED
        );

        documentStorage.store(document.documentId(), input.fileContent());
        Document savedDocument = documentRepository.save(document);
        return new UploadDocumentOutput(savedDocument.documentId(), savedDocument.status(), "Document uploaded successfully");
    }

    private String calculateContentHash(byte[] fileContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(fileContent));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}

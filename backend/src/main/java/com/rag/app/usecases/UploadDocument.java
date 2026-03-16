package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.usecases.models.ProcessDocumentInput;
import com.rag.app.usecases.models.UploadDocumentInput;
import com.rag.app.usecases.models.UploadDocumentOutput;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public final class UploadDocument {
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ProcessDocument processDocument;
    private final Clock clock;
    
    @ConfigProperty(name = "quarkus.profile", defaultValue = "prod")
    String profile;

    @Inject
    public UploadDocument(DocumentRepository documentRepository, UserRepository userRepository, ProcessDocument processDocument) {
        this(documentRepository, userRepository, processDocument, Clock.systemUTC());
    }

    public UploadDocument(DocumentRepository documentRepository, UserRepository userRepository, ProcessDocument processDocument, Clock clock) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.processDocument = Objects.requireNonNull(processDocument, "processDocument must not be null");
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
        if (input.fileSize() <= 0 || input.fileSize() > DocumentMetadata.MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("fileSize must be positive and less than or equal to 41943040");
        }
        if (input.fileContent() == null || input.fileContent().length == 0) {
            throw new IllegalArgumentException("fileContent must not be null or empty");
        }
        if (input.uploadedBy() == null) {
            throw new IllegalArgumentException("uploadedBy must not be null");
        }

        User user = userRepository.findById(input.uploadedBy())
            .orElseThrow(() -> new IllegalArgumentException("uploadedBy user must exist"));

        if (!user.isActive()) {
            throw new IllegalArgumentException("uploadedBy user must be active");
        }

        String contentHash = calculateContentHash(input.fileContent());
        
        // In development mode, make content hash unique to allow duplicate uploads
        if ("dev".equals(profile)) {
            contentHash = contentHash + "-" + Instant.now(clock).toEpochMilli();
        }
        
        if (documentRepository.findByContentHash(contentHash).isPresent()) {
            throw new IllegalArgumentException("document with identical content already exists");
        }

        Document document = new Document(
            UUID.randomUUID(),
            new DocumentMetadata(input.fileName(), input.fileSize(), input.fileType(), contentHash),
            input.uploadedBy().toString(),
            Instant.now(clock),
            DocumentStatus.UPLOADED
        );

        Document savedDocument = documentRepository.save(document);
        
        // Automatically trigger document processing
        try {
            processDocument.execute(new ProcessDocumentInput(savedDocument.documentId(), input.fileContent()));
            return new UploadDocumentOutput(savedDocument.documentId(), DocumentStatus.PROCESSING, "Document uploaded and processing started");
        } catch (Exception exception) {
            // Mark document as FAILED so it is not stuck in UPLOADED
            String errorMessage = exception.getMessage();
            if (exception.getCause() != null) {
                errorMessage += " (Cause: " + exception.getCause().getMessage() + ")";
            }
            documentRepository.save(savedDocument.withStatus(DocumentStatus.FAILED));
            return new UploadDocumentOutput(savedDocument.documentId(), DocumentStatus.FAILED, "Document uploaded successfully, but processing failed: " + errorMessage);
        }
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

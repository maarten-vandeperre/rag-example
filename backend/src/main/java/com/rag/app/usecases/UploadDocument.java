package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.usecases.models.UploadDocumentInput;
import com.rag.app.usecases.models.UploadDocumentOutput;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
    private final Clock clock;

    @Inject
    public UploadDocument(DocumentRepository documentRepository, UserRepository userRepository) {
        this(documentRepository, userRepository, Clock.systemUTC());
    }

    public UploadDocument(DocumentRepository documentRepository, UserRepository userRepository, Clock clock) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
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

package com.rag.app.document.usecases;

import com.rag.app.document.domain.entities.Document;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.interfaces.DocumentContentExtractor;
import com.rag.app.document.interfaces.DocumentRepository;
import com.rag.app.document.interfaces.DocumentStorage;
import com.rag.app.document.interfaces.DocumentVectorStore;
import com.rag.app.document.usecases.models.ProcessDocumentInput;
import com.rag.app.document.usecases.models.ProcessDocumentOutput;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class ProcessDocument {
    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final DocumentContentExtractor documentContentExtractor;
    private final DocumentVectorStore documentVectorStore;
    private final Clock clock;

    public ProcessDocument(DocumentRepository documentRepository,
                           DocumentStorage documentStorage,
                           DocumentContentExtractor documentContentExtractor,
                           DocumentVectorStore documentVectorStore,
                           Clock clock) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.documentStorage = Objects.requireNonNull(documentStorage, "documentStorage must not be null");
        this.documentContentExtractor = Objects.requireNonNull(documentContentExtractor, "documentContentExtractor must not be null");
        this.documentVectorStore = Objects.requireNonNull(documentVectorStore, "documentVectorStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ProcessDocumentOutput execute(ProcessDocumentInput input) {
        Objects.requireNonNull(input, "input must not be null");
        if (input.documentId() == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }

        Document document = documentRepository.findById(input.documentId())
            .orElseThrow(() -> new IllegalArgumentException("document must exist"));

        byte[] fileContent = documentStorage.load(input.documentId())
            .orElseThrow(() -> new IllegalArgumentException("fileContent must exist for document"));

        Instant now = Instant.now(clock);
        documentRepository.save(document.withStatus(DocumentStatus.PROCESSING, now));

        try {
            String extractedText = documentContentExtractor.extractText(fileContent, document.fileType());
            if (extractedText == null || extractedText.isBlank()) {
                documentRepository.save(document.withStatus(DocumentStatus.FAILED, Instant.now(clock)));
                return new ProcessDocumentOutput(document.documentId(), DocumentStatus.FAILED, 0, "No usable content extracted from document");
            }

            documentVectorStore.storeDocument(document.documentId(), extractedText);
            documentRepository.save(document.withStatus(DocumentStatus.READY, Instant.now(clock)));
            return new ProcessDocumentOutput(document.documentId(), DocumentStatus.READY, extractedText.length(), null);
        } catch (RuntimeException exception) {
            documentRepository.save(document.withStatus(DocumentStatus.FAILED, Instant.now(clock)));
            return new ProcessDocumentOutput(document.documentId(), DocumentStatus.FAILED, 0, exception.getMessage());
        }
    }
}

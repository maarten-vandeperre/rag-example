package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.usecases.interfaces.DocumentContentExtractor;
import com.rag.app.usecases.interfaces.VectorStore;
import com.rag.app.usecases.models.ProcessDocumentInput;
import com.rag.app.usecases.models.ProcessDocumentOutput;
import com.rag.app.usecases.repositories.DocumentRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;

@ApplicationScoped
public final class ProcessDocument {
    private final DocumentRepository documentRepository;
    private final DocumentContentExtractor documentContentExtractor;
    private final VectorStore vectorStore;

    @Inject
    public ProcessDocument(DocumentRepository documentRepository,
                           DocumentContentExtractor documentContentExtractor,
                           VectorStore vectorStore) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.documentContentExtractor = Objects.requireNonNull(documentContentExtractor, "documentContentExtractor must not be null");
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
    }

    public ProcessDocumentOutput execute(ProcessDocumentInput input) {
        Objects.requireNonNull(input, "input must not be null");

        if (input.documentId() == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        if (input.fileContent() == null || input.fileContent().length == 0) {
            throw new IllegalArgumentException("fileContent must not be null or empty");
        }

        Document document = documentRepository.findById(input.documentId())
            .orElseThrow(() -> new IllegalArgumentException("document must exist"));

        document = documentRepository.save(document.withStatus(DocumentStatus.PROCESSING));

        try {
            String extractedText = documentContentExtractor.extractText(input.fileContent(), document.fileType());

            if (extractedText == null || extractedText.isBlank()) {
                documentRepository.save(document.withStatus(DocumentStatus.FAILED));
                return new ProcessDocumentOutput(document.documentId(), DocumentStatus.FAILED, 0, "No usable content extracted from document");
            }

            vectorStore.storeDocumentVectors(document.documentId().toString(), extractedText);
            documentRepository.save(document.withStatus(DocumentStatus.READY));
            return new ProcessDocumentOutput(document.documentId(), DocumentStatus.READY, extractedText.length(), null);
        } catch (RuntimeException exception) {
            documentRepository.save(document.withStatus(DocumentStatus.FAILED));
            return new ProcessDocumentOutput(document.documentId(), DocumentStatus.FAILED, 0, exception.getMessage());
        }
    }
}

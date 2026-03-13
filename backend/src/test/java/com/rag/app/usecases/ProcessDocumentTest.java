package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.usecases.interfaces.VectorStore;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.ProcessDocumentInput;
import com.rag.app.usecases.models.ProcessDocumentOutput;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;
import com.rag.app.usecases.repositories.DocumentRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessDocumentTest {

    @Test
    void shouldMarkDocumentReadyWhenProcessingSucceeds() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = uploadedDocument();
        repository.save(document);
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        ProcessDocument useCase = new ProcessDocument(repository, (content, fileType) -> "Extracted text", vectorStore);

        ProcessDocumentOutput output = useCase.execute(new ProcessDocumentInput(document.documentId(), new byte[]{1, 2, 3}));

        assertEquals(DocumentStatus.READY, output.finalStatus());
        assertEquals(14, output.extractedTextLength());
        assertEquals(null, output.errorMessage());
        assertEquals(DocumentStatus.READY, repository.findById(document.documentId()).orElseThrow().status());
        assertEquals(document.documentId().toString(), vectorStore.documentId);
        assertEquals("Extracted text", vectorStore.text);
    }

    @Test
    void shouldMarkDocumentFailedWhenExtractionReturnsNoUsableContent() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = uploadedDocument();
        repository.save(document);
        ProcessDocument useCase = new ProcessDocument(repository, (content, fileType) -> "   ", (documentId, text) -> { });

        ProcessDocumentOutput output = useCase.execute(new ProcessDocumentInput(document.documentId(), new byte[]{1}));

        assertEquals(DocumentStatus.FAILED, output.finalStatus());
        assertEquals(0, output.extractedTextLength());
        assertEquals("No usable content extracted from document", output.errorMessage());
        assertEquals(DocumentStatus.FAILED, repository.findById(document.documentId()).orElseThrow().status());
    }

    @Test
    void shouldMarkDocumentFailedWhenExtractionThrows() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = uploadedDocument();
        repository.save(document);
        ProcessDocument useCase = new ProcessDocument(repository, (content, fileType) -> {
            throw new IllegalStateException("Extraction failed");
        }, (documentId, text) -> { });

        ProcessDocumentOutput output = useCase.execute(new ProcessDocumentInput(document.documentId(), new byte[]{1}));

        assertEquals(DocumentStatus.FAILED, output.finalStatus());
        assertEquals("Extraction failed", output.errorMessage());
        assertEquals(DocumentStatus.FAILED, repository.findById(document.documentId()).orElseThrow().status());
    }

    @Test
    void shouldMarkDocumentFailedWhenVectorStorageThrows() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = uploadedDocument();
        repository.save(document);
        ProcessDocument useCase = new ProcessDocument(repository, (content, fileType) -> "Extracted text", (documentId, text) -> {
            throw new IllegalStateException("Vector storage failed");
        });

        ProcessDocumentOutput output = useCase.execute(new ProcessDocumentInput(document.documentId(), new byte[]{1}));

        assertEquals(DocumentStatus.FAILED, output.finalStatus());
        assertEquals("Vector storage failed", output.errorMessage());
        assertEquals(DocumentStatus.FAILED, repository.findById(document.documentId()).orElseThrow().status());
    }

    @Test
    void shouldRejectMissingDocumentId() {
        ProcessDocument useCase = new ProcessDocument(new InMemoryDocumentRepository(), (content, fileType) -> "text", (documentId, text) -> { });

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new ProcessDocumentInput(null, new byte[]{1})));

        assertEquals("documentId must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectUnknownDocuments() {
        ProcessDocument useCase = new ProcessDocument(new InMemoryDocumentRepository(), (content, fileType) -> "text", (documentId, text) -> { });

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new ProcessDocumentInput(UUID.randomUUID(), new byte[]{1})));

        assertEquals("document must exist", exception.getMessage());
    }

    private static Document uploadedDocument() {
        return new Document(
            UUID.randomUUID(),
            new DocumentMetadata("guide.pdf", 123L, FileType.PDF, "hash-123"),
            UUID.randomUUID().toString(),
            Instant.parse("2026-03-13T15:00:00Z"),
            DocumentStatus.UPLOADED
        );
    }

    private static final class InMemoryDocumentRepository implements DocumentRepository {
        private final Map<UUID, Document> documents = new ConcurrentHashMap<>();

        @Override
        public Document save(Document document) {
            documents.put(document.documentId(), document);
            return document;
        }

        @Override
        public Optional<Document> findByContentHash(String hash) {
            return documents.values().stream().filter(document -> document.contentHash().equals(hash)).findFirst();
        }

        @Override
        public Optional<Document> findById(UUID documentId) {
            return Optional.ofNullable(documents.get(documentId));
        }

        @Override
        public List<Document> findByUploadedBy(String userId) {
            return documents.values().stream()
                .filter(document -> document.uploadedBy().equals(userId))
                .toList();
        }

        @Override
        public List<Document> findAll() {
            return documents.values().stream().toList();
        }

        @Override
        public List<Document> findByStatus(DocumentStatus status) {
            return documents.values().stream()
                .filter(document -> document.status() == status)
                .toList();
        }

        @Override
        public ProcessingStatistics getProcessingStatistics() {
            return new ProcessingStatistics(documents.size(), 0, 0, 0, 0);
        }

        @Override
        public List<FailedDocumentInfo> findFailedDocuments() {
            return List.of();
        }

        @Override
        public List<ProcessingDocumentInfo> findProcessingDocuments() {
            return List.of();
        }

        @Override
        public void updateStatus(UUID documentId, DocumentStatus status) {
            findById(documentId).ifPresent(document -> documents.put(documentId, document.withStatus(status)));
        }
    }

    private static final class RecordingVectorStore implements VectorStore {
        private String documentId;
        private String text;

        @Override
        public void storeDocumentVectors(String documentId, String text) {
            this.documentId = documentId;
            this.text = text;
        }
    }
}

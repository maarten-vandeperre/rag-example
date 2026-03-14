package com.rag.app.infrastructure.vector;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.DocumentChunk;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorStoreImplTest {

    @Test
    void shouldStoreChunksAndReturnRelevantMatches() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document productGuide = document("product-guide.txt");
        Document financeGuide = document("finance-guide.txt");
        repository.save(productGuide);
        repository.save(financeGuide);
        VectorStoreImpl vectorStore = new VectorStoreImpl(
            repository,
            new TextChunker(40, 80, 15),
            new EmbeddingGenerator(64),
            new ConcurrentHashMap<>()
        );

        vectorStore.storeDocumentVectors(productGuide.documentId().toString(),
            "Upload workflows stay reliable for product teams. Search flows answer chat questions quickly.");
        vectorStore.storeDocumentVectors(financeGuide.documentId().toString(),
            "Invoices, ledgers, and reconciliation steps belong to finance workflows.");

        List<DocumentChunk> results = vectorStore.searchDocuments("How do upload workflows support chat search?",
            List.of(productGuide.documentId().toString()));

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(chunk -> chunk.documentId().equals(productGuide.documentId())));
        assertEquals("product-guide.txt", results.get(0).documentName());
        assertTrue(results.get(0).relevanceScore() >= results.get(1).relevanceScore());
        assertTrue(results.get(0).embedding().length > 0);
    }

    @Test
    void shouldFilterOutDocumentsOutsideRequestedIds() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document allowed = document("allowed.txt");
        Document excluded = document("excluded.txt");
        repository.save(allowed);
        repository.save(excluded);
        VectorStoreImpl vectorStore = new VectorStoreImpl(
            repository,
            new TextChunker(20, 60, 10),
            new EmbeddingGenerator(64),
            new ConcurrentHashMap<>()
        );

        vectorStore.storeDocumentVectors(allowed.documentId().toString(), "Allowed content about upload processing.");
        vectorStore.storeDocumentVectors(excluded.documentId().toString(), "Upload processing for excluded content.");

        List<DocumentChunk> results = vectorStore.searchDocuments("upload processing", List.of(allowed.documentId().toString()));

        assertEquals(1, results.size());
        assertEquals(allowed.documentId(), results.get(0).documentId());
    }

    @Test
    void shouldReturnEmptyResultsWhenQueryHasNoRelevantMatch() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = document("guide.txt");
        repository.save(document);
        VectorStoreImpl vectorStore = new VectorStoreImpl(
            repository,
            new TextChunker(20, 60, 10),
            new EmbeddingGenerator(64),
            new ConcurrentHashMap<>()
        );

        vectorStore.storeDocumentVectors(document.documentId().toString(), "Upload processing and indexing steps.");

        List<DocumentChunk> results = vectorStore.searchDocuments("astronomy telescope orbit", List.of(document.documentId().toString()));

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldThrowHelpfulErrorWhenStorageFails() {
        VectorStoreImpl vectorStore = new VectorStoreImpl(
            new InMemoryDocumentRepository(),
            new TextChunker(20, 60, 10),
            new EmbeddingGenerator(64),
            new ConcurrentHashMap<>()
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> vectorStore.storeDocumentVectors(UUID.randomUUID().toString(), "Some content"));

        assertEquals("Failed to store document vectors", exception.getMessage());
    }

    private static Document document(String fileName) {
        UUID documentId = UUID.randomUUID();
        return new Document(
            documentId,
            new DocumentMetadata(fileName, 128L, FileType.PLAIN_TEXT, "hash-" + documentId),
            UUID.randomUUID().toString(),
            Instant.parse("2026-03-13T15:00:00Z"),
            DocumentStatus.READY
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
            return documents.values().stream().filter(document -> document.uploadedBy().equals(userId)).toList();
        }

        @Override
        public List<Document> findAll() {
            return documents.values().stream().toList();
        }

        @Override
        public List<Document> findByStatus(DocumentStatus status) {
            return documents.values().stream().filter(document -> document.status() == status).toList();
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
}

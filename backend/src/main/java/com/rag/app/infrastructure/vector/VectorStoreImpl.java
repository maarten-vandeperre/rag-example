package com.rag.app.infrastructure.vector;

import com.rag.app.domain.entities.Document;
import com.rag.app.usecases.interfaces.SemanticSearch;
import com.rag.app.usecases.interfaces.VectorStore;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.repositories.DocumentRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class VectorStoreImpl implements VectorStore, SemanticSearch {
    private static final int DEFAULT_TOP_K = 5;
    private static final double MIN_RELEVANCE_SCORE = 0.2d;

    private final DocumentRepository documentRepository;
    private final TextChunker textChunker;
    private final EmbeddingGenerator embeddingGenerator;
    private final Map<String, List<DocumentChunk>> chunksByDocumentId;

    @Inject
    public VectorStoreImpl(DocumentRepository documentRepository,
                           TextChunker textChunker,
                           EmbeddingGenerator embeddingGenerator) {
        this(documentRepository, textChunker, embeddingGenerator, new ConcurrentHashMap<>());
    }

    VectorStoreImpl(DocumentRepository documentRepository,
                    TextChunker textChunker,
                    EmbeddingGenerator embeddingGenerator,
                    Map<String, List<DocumentChunk>> chunksByDocumentId) {
        this.documentRepository = documentRepository;
        this.textChunker = textChunker;
        this.embeddingGenerator = embeddingGenerator;
        this.chunksByDocumentId = chunksByDocumentId;
    }

    @Override
    public void storeDocumentVectors(String documentId, String text) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be null or empty");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be null or empty");
        }

        try {
            Document document = documentRepository.findById(UUID.fromString(documentId))
                .orElseThrow(() -> new IllegalArgumentException("document must exist"));

            List<String> chunks = textChunker.chunk(text);
            List<DocumentChunk> storedChunks = new ArrayList<>(chunks.size());
            for (int index = 0; index < chunks.size(); index++) {
                String chunkText = chunks.get(index);
                storedChunks.add(new DocumentChunk(
                    document.documentId() + "-chunk-" + index,
                    document.documentId(),
                    document.fileName(),
                    index,
                    "chunk-" + (index + 1),
                    chunkText,
                    embeddingGenerator.generateEmbedding(chunkText),
                    0.0d
                ));
            }

            chunksByDocumentId.put(documentId, List.copyOf(storedChunks));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Failed to store document vectors", exception);
        }
    }

    @Override
    public List<DocumentChunk> searchDocuments(String query, List<String> documentIds) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be null or empty");
        }
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }

        double[] queryEmbedding = embeddingGenerator.generateEmbedding(query);

        return documentIds.stream()
            .map(chunksByDocumentId::get)
            .filter(storedChunks -> storedChunks != null && !storedChunks.isEmpty())
            .flatMap(List::stream)
            .map(chunk -> scoredChunk(chunk, queryEmbedding))
            .filter(chunk -> chunk.relevanceScore() >= MIN_RELEVANCE_SCORE)
            .sorted(Comparator.comparingDouble(DocumentChunk::relevanceScore).reversed())
            .limit(DEFAULT_TOP_K)
            .toList();
    }

    private DocumentChunk scoredChunk(DocumentChunk chunk, double[] queryEmbedding) {
        double score = cosineSimilarity(queryEmbedding, chunk.embedding());
        return new DocumentChunk(
            chunk.chunkId(),
            chunk.documentId(),
            chunk.documentName(),
            chunk.chunkIndex(),
            chunk.paragraphReference(),
            chunk.text(),
            chunk.embedding(),
            score
        );
    }

    private double cosineSimilarity(double[] left, double[] right) {
        double dotProduct = 0.0d;
        int dimensions = Math.min(left.length, right.length);
        for (int index = 0; index < dimensions; index++) {
            dotProduct += left[index] * right[index];
        }
        return dotProduct;
    }
}

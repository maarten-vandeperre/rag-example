package com.rag.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.app.infrastructure.vector.VectorStoreImpl;
import com.rag.app.infrastructure.vector.WeaviateVectorStore;
import com.rag.app.usecases.interfaces.DocumentChunkStore;
import com.rag.app.usecases.interfaces.SemanticSearch;
import com.rag.app.usecases.interfaces.VectorStore;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.infrastructure.vector.TextChunker;
import com.rag.app.infrastructure.vector.EmbeddingGenerator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class VectorStoreConfiguration {
    private static final Logger LOG = Logger.getLogger(VectorStoreConfiguration.class);

    @ConfigProperty(name = "app.vectorstore.provider", defaultValue = "memory")
    String vectorStoreProvider;

    @ConfigProperty(name = "app.vectorstore.url", defaultValue = "http://localhost:8080")
    String vectorStoreUrl;

    @ConfigProperty(name = "app.vectorstore.timeout", defaultValue = "30000")
    int vectorStoreTimeoutMs;

    @ConfigProperty(name = "app.vector.max-results", defaultValue = "10")
    int maxResults;

    @ConfigProperty(name = "app.vector.similarity-threshold", defaultValue = "0.2")
    double similarityThreshold;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    TextChunker textChunker;

    @Inject
    EmbeddingGenerator embeddingGenerator;

    @Inject
    ObjectMapper objectMapper;

    @Produces
    @Singleton
    @Named("documentChunksMap")
    public Map<String, List<DocumentChunk>> documentChunksMap() {
        return new ConcurrentHashMap<>();
    }

    @Produces
    @Singleton
    public VectorStore vectorStore(@Named("documentChunksMap") Map<String, List<DocumentChunk>> chunksByDocumentId) {
        LOG.infof("Configuring VectorStore with provider: %s", vectorStoreProvider);
        
        if ("weaviate".equalsIgnoreCase(vectorStoreProvider)) {
            LOG.infof("Using Weaviate VectorStore at %s", vectorStoreUrl);
            return new WeaviateVectorStore(
                documentRepository,
                textChunker,
                embeddingGenerator,
                objectMapper,
                vectorStoreUrl,
                Duration.ofMillis(vectorStoreTimeoutMs),
                maxResults,
                similarityThreshold
            );
        }
        
        LOG.info("Using in-memory VectorStore implementation");
        return new VectorStoreImpl(documentRepository, textChunker, embeddingGenerator, chunksByDocumentId);
    }

    @Produces
    @Singleton
    public SemanticSearch semanticSearch(VectorStore vectorStore) {
        // VectorStoreImpl implements both VectorStore and SemanticSearch
        return (SemanticSearch) vectorStore;
    }

    @Produces
    @Singleton
    public DocumentChunkStore documentChunkStore(VectorStore vectorStore) {
        // VectorStoreImpl implements VectorStore, SemanticSearch, and DocumentChunkStore
        return (DocumentChunkStore) vectorStore;
    }
}

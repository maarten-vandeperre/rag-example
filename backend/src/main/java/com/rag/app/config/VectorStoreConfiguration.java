package com.rag.app.config;

import com.rag.app.infrastructure.vector.VectorStoreImpl;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class VectorStoreConfiguration {
    private static final Logger LOG = Logger.getLogger(VectorStoreConfiguration.class);

    @ConfigProperty(name = "app.vectorstore.provider", defaultValue = "memory")
    String vectorStoreProvider;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    TextChunker textChunker;

    @Inject
    EmbeddingGenerator embeddingGenerator;

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
            LOG.info("Weaviate VectorStore requested but not yet implemented, falling back to in-memory implementation");
            // TODO: Implement WeaviateVectorStoreImpl when Weaviate client dependency is available
            // return new WeaviateVectorStoreImpl(documentRepository, textChunker);
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
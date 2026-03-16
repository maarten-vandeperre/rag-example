package com.rag.app.infrastructure.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.app.domain.entities.Document;
import com.rag.app.usecases.interfaces.DocumentChunkStore;
import com.rag.app.usecases.interfaces.SemanticSearch;
import com.rag.app.usecases.interfaces.VectorStore;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.repositories.DocumentRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

public final class WeaviateVectorStore implements VectorStore, SemanticSearch, DocumentChunkStore {
    private static final String DOCUMENT_CHUNK_CLASS = "DocumentChunk";
    private static final Logger LOG = Logger.getLogger(WeaviateVectorStore.class);

    private final DocumentRepository documentRepository;
    private final TextChunker textChunker;
    private final EmbeddingGenerator embeddingGenerator;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI objectsUri;
    private final URI graphqlUri;
    private final Map<String, List<DocumentChunk>> localFallbackChunks;
    private final int maxResults;
    private final double similarityThreshold;

    public WeaviateVectorStore(DocumentRepository documentRepository,
                               TextChunker textChunker,
                               EmbeddingGenerator embeddingGenerator,
                               ObjectMapper objectMapper,
                               String baseUrl,
                               Duration timeout,
                               int maxResults,
                               double similarityThreshold) {
        this(
            documentRepository,
            textChunker,
            embeddingGenerator,
            objectMapper,
            HttpClient.newBuilder().connectTimeout(timeout).build(),
            URI.create(normalizeBaseUrl(baseUrl) + "/v1/objects"),
            URI.create(normalizeBaseUrl(baseUrl) + "/v1/graphql"),
            new ConcurrentHashMap<>(),
            maxResults,
            similarityThreshold
        );
    }

    WeaviateVectorStore(DocumentRepository documentRepository,
                        TextChunker textChunker,
                        EmbeddingGenerator embeddingGenerator,
                        ObjectMapper objectMapper,
                        HttpClient httpClient,
                        URI objectsUri,
                        URI graphqlUri,
                        Map<String, List<DocumentChunk>> localFallbackChunks,
                        int maxResults,
                        double similarityThreshold) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.textChunker = Objects.requireNonNull(textChunker, "textChunker must not be null");
        this.embeddingGenerator = Objects.requireNonNull(embeddingGenerator, "embeddingGenerator must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectsUri = Objects.requireNonNull(objectsUri, "objectsUri must not be null");
        this.graphqlUri = Objects.requireNonNull(graphqlUri, "graphqlUri must not be null");
        this.localFallbackChunks = Objects.requireNonNull(localFallbackChunks, "localFallbackChunks must not be null");
        if (maxResults <= 0) {
            throw new IllegalArgumentException("maxResults must be positive");
        }
        this.maxResults = maxResults;
        this.similarityThreshold = similarityThreshold;
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
            List<DocumentChunk> chunks = buildChunks(document, text);
            localFallbackChunks.put(documentId, List.copyOf(chunks));
            try {
                replaceRemoteChunks(document, chunks);
            } catch (RuntimeException exception) {
                LOG.warnf(exception,
                    "Failed to synchronize document %s to Weaviate. Using local fallback chunks for this runtime.",
                    documentId);
            }
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
        try {
            List<DocumentChunk> remoteResults = searchRemote(queryEmbedding, documentIds);
            if (!remoteResults.isEmpty()) {
                return remoteResults;
            }
        } catch (RuntimeException ignored) {
        }

        return fallbackSearch(queryEmbedding, documentIds);
    }

    @Override
    public List<DocumentChunk> getDocumentChunks(UUID documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }

        try {
            List<DocumentChunk> remoteChunks = loadRemoteChunks(documentId.toString());
            if (!remoteChunks.isEmpty()) {
                return remoteChunks;
            }
        } catch (RuntimeException ignored) {
        }

        return List.copyOf(localFallbackChunks.getOrDefault(documentId.toString(), List.of()));
    }

    private List<DocumentChunk> buildChunks(Document document, String text) {
        List<String> chunkTexts = textChunker.chunk(text);
        List<DocumentChunk> chunks = new ArrayList<>(chunkTexts.size());
        for (int index = 0; index < chunkTexts.size(); index++) {
            String chunkText = chunkTexts.get(index);
            chunks.add(new DocumentChunk(
                deterministicChunkObjectId(document.documentId(), index),
                document.documentId(),
                document.fileName(),
                index,
                "chunk-" + (index + 1),
                chunkText,
                embeddingGenerator.generateEmbedding(chunkText),
                0.0d
            ));
        }
        return chunks;
    }

    private void replaceRemoteChunks(Document document, List<DocumentChunk> chunks) {
        deleteRemoteChunks(document.documentId());
        chunks.forEach(chunk -> storeRemoteChunk(document, chunk));
    }

    private void deleteRemoteChunks(UUID documentId) {
        try {
            for (DocumentChunk existingChunk : loadRemoteChunks(documentId.toString())) {
                HttpRequest deleteRequest = HttpRequest.newBuilder(objectsUri.resolve("/v1/objects/" + existingChunk.chunkId()))
                    .DELETE()
                    .build();
                httpClient.send(deleteRequest, HttpResponse.BodyHandlers.discarding());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete existing Weaviate chunks", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while deleting Weaviate chunks", exception);
        }
    }

    private void storeRemoteChunk(Document document, DocumentChunk chunk) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("class", DOCUMENT_CHUNK_CLASS);
        payload.put("id", chunk.chunkId());
        payload.put("vector", chunk.embedding());
        payload.put("properties", Map.of(
            "documentId", document.documentId().toString(),
            "chunkIndex", chunk.chunkIndex(),
            "textContent", chunk.text(),
            "uploadedBy", document.uploadedBy(),
            "fileName", document.fileName(),
            "fileType", document.fileType().name(),
            "createdAt", document.uploadedAt().toString(),
            "chunkSize", chunk.text().length()
        ));

        try {
            HttpRequest request = HttpRequest.newBuilder(objectsUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Weaviate object write failed with status " + response.statusCode());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write document chunk to Weaviate", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while writing document chunk to Weaviate", exception);
        }
    }

    private List<DocumentChunk> searchRemote(double[] queryEmbedding, List<String> documentIds) {
        String query = "{ Get { DocumentChunk(nearVector: {vector: %s}, where: %s, limit: %d) { documentId chunkIndex textContent fileName _additional { id distance } } } }"
            .formatted(toVectorLiteral(queryEmbedding), toDocumentIdWhere(documentIds), maxResults);
        JsonNode response = executeGraphQl(query);
        return parseChunks(response.path("data").path("Get").path(DOCUMENT_CHUNK_CLASS), true);
    }

    private List<DocumentChunk> loadRemoteChunks(String documentId) {
        String query = "{ Get { DocumentChunk(where: {path: [\"documentId\"], operator: Equal, valueText: \"%s\"}, limit: %d) { documentId chunkIndex textContent fileName _additional { id } } } }"
            .formatted(documentId, Math.max(maxResults * 5, 100));
        JsonNode response = executeGraphQl(query);
        return parseChunks(response.path("data").path("Get").path(DOCUMENT_CHUNK_CLASS), false).stream()
            .sorted(Comparator.comparingInt(DocumentChunk::chunkIndex))
            .toList();
    }

    private JsonNode executeGraphQl(String query) {
        try {
            HttpRequest request = HttpRequest.newBuilder(graphqlUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("query", query)), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Weaviate GraphQL request failed with status " + response.statusCode());
            }
            JsonNode body = objectMapper.readTree(response.body());
            if (body.has("errors") && body.get("errors").isArray() && body.get("errors").size() > 0) {
                throw new IllegalStateException("Weaviate GraphQL returned errors");
            }
            return body;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to execute Weaviate GraphQL request", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during Weaviate GraphQL request", exception);
        }
    }

    private List<DocumentChunk> parseChunks(JsonNode nodes, boolean includeDistance) {
        if (!nodes.isArray()) {
            return List.of();
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        for (JsonNode node : nodes) {
            JsonNode additional = node.path("_additional");
            double relevanceScore = includeDistance && additional.has("distance")
                ? Math.max(0.0d, 1.0d - additional.path("distance").asDouble(1.0d))
                : 0.0d;
            if (includeDistance && relevanceScore < similarityThreshold) {
                continue;
            }
            UUID documentId = UUID.fromString(node.path("documentId").asText());
            int chunkIndex = node.path("chunkIndex").asInt();
            chunks.add(new DocumentChunk(
                additional.path("id").asText(deterministicChunkObjectId(documentId, chunkIndex)),
                documentId,
                node.path("fileName").asText(resolveDocumentName(documentId)),
                chunkIndex,
                "chunk-" + (chunkIndex + 1),
                node.path("textContent").asText(),
                new double[0],
                relevanceScore
            ));
        }
        return List.copyOf(chunks);
    }

    private List<DocumentChunk> fallbackSearch(double[] queryEmbedding, List<String> documentIds) {
        return documentIds.stream()
            .map(localFallbackChunks::get)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .map(chunk -> scoredChunk(chunk, queryEmbedding))
            .filter(chunk -> chunk.relevanceScore() >= similarityThreshold)
            .sorted(Comparator.comparingDouble(DocumentChunk::relevanceScore).reversed())
            .limit(maxResults)
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

    private String resolveDocumentName(UUID documentId) {
        return documentRepository.findById(documentId).map(Document::fileName).orElse(documentId.toString());
    }

    private String toVectorLiteral(double[] vector) {
        return java.util.Arrays.stream(vector)
            .mapToObj(value -> Double.toString(value))
            .collect(Collectors.joining(",", "[", "]"));
    }

    private String toDocumentIdWhere(List<String> documentIds) {
        if (documentIds.size() == 1) {
            return "{path: [\"documentId\"], operator: Equal, valueText: \"%s\"}".formatted(documentIds.get(0));
        }

        String operands = documentIds.stream()
            .map(documentId -> "{path: [\"documentId\"], operator: Equal, valueText: \"%s\"}".formatted(documentId))
            .collect(Collectors.joining(","));
        return "{operator: Or, operands: [%s]}".formatted(operands);
    }

    private String deterministicChunkObjectId(UUID documentId, int chunkIndex) {
        return UUID.nameUUIDFromBytes((documentId + ":chunk:" + chunkIndex).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be null or empty");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}

package com.rag.app.infrastructure.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rag.app.domain.entities.Document;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaviateVectorStoreTest {

    @Test
    void shouldStoreChunksAndSearchThroughWeaviate() throws Exception {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document allowed = document("allowed.txt");
        Document excluded = document("excluded.txt");
        repository.save(allowed);
        repository.save(excluded);

        try (StubWeaviateServer server = new StubWeaviateServer()) {
            WeaviateVectorStore vectorStore = new WeaviateVectorStore(
                repository,
                new TextChunker(20, 60, 10),
                new EmbeddingGenerator(64),
                new ObjectMapper(),
                HttpClient.newHttpClient(),
                URI.create(server.baseUrl() + "/v1/objects"),
                URI.create(server.baseUrl() + "/v1/graphql"),
                new ConcurrentHashMap<>(),
                5,
                0.2d
            );

            vectorStore.storeDocumentVectors(allowed.documentId().toString(), "Upload workflows support chat search and document retrieval.");
            vectorStore.storeDocumentVectors(excluded.documentId().toString(), "Finance workflows do not explain upload chat behavior.");

            List<DocumentChunk> results = vectorStore.searchDocuments("How do upload workflows help chat?", List.of(allowed.documentId().toString()));

            assertFalse(results.isEmpty());
            assertTrue(results.stream().allMatch(chunk -> chunk.documentId().equals(allowed.documentId())));
            assertEquals("allowed.txt", results.get(0).documentName());
            assertFalse(vectorStore.getDocumentChunks(allowed.documentId()).isEmpty());
        }
    }

    @Test
    void shouldFallbackToLocalChunksWhenWeaviateIsUnavailable() throws Exception {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = document("guide.txt");
        repository.save(document);

        try (StubWeaviateServer server = new StubWeaviateServer()) {
            WeaviateVectorStore vectorStore = new WeaviateVectorStore(
                repository,
                new TextChunker(20, 60, 10),
                new EmbeddingGenerator(64),
                new ObjectMapper(),
                HttpClient.newHttpClient(),
                URI.create(server.baseUrl() + "/v1/objects"),
                URI.create(server.baseUrl() + "/v1/graphql"),
                new ConcurrentHashMap<>(),
                5,
                0.2d
            );

            vectorStore.storeDocumentVectors(document.documentId().toString(), "Upload workflows stay searchable even if Weaviate is down.");
            server.stopServing();

            List<DocumentChunk> results = vectorStore.searchDocuments("searchable upload workflows", List.of(document.documentId().toString()));

            assertFalse(results.isEmpty());
            assertEquals(document.documentId(), results.get(0).documentId());
        }
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

    private static final class StubWeaviateServer implements AutoCloseable {
        private static final Pattern VALUE_TEXT_PATTERN = Pattern.compile("valueText: \\\"([^\\\"]+)\\\"");
        private static final Pattern LIMIT_PATTERN = Pattern.compile("limit: (\\d+)");
        private static final Pattern VECTOR_PATTERN = Pattern.compile("vector: \\[(.*?)\\]", Pattern.DOTALL);

        private final ObjectMapper objectMapper = new ObjectMapper();
        private final Map<String, StoredObject> storedObjects = new LinkedHashMap<>();
        private final HttpServer server;
        private volatile boolean serving = true;

        private StubWeaviateServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/v1/objects", this::handleObjects);
            server.createContext("/v1/graphql", this::handleGraphQl);
            server.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void stopServing() {
            serving = false;
        }

        private void handleObjects(HttpExchange exchange) throws IOException {
            if (!serving) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                JsonNode body = objectMapper.readTree(exchange.getRequestBody());
                String id = body.path("id").asText();
                JsonNode properties = body.path("properties");
                storedObjects.put(id, new StoredObject(
                    id,
                    properties.path("documentId").asText(),
                    properties.path("fileName").asText(),
                    properties.path("textContent").asText(),
                    properties.path("chunkIndex").asInt(),
                    toVector(body.path("vector"))
                ));
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }

            if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                String path = exchange.getRequestURI().getPath();
                storedObjects.remove(path.substring(path.lastIndexOf('/') + 1));
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        }

        private void handleGraphQl(HttpExchange exchange) throws IOException {
            if (!serving) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
            }

            JsonNode body = objectMapper.readTree(exchange.getRequestBody());
            String query = body.path("query").asText();
            List<String> documentIds = extractDocumentIds(query);
            int limit = extractLimit(query);

            List<StoredObject> filtered = storedObjects.values().stream()
                .filter(object -> documentIds.isEmpty() || documentIds.contains(object.documentId))
                .toList();

            ArrayNode chunksNode = objectMapper.createArrayNode();
            if (query.contains("nearVector")) {
                double[] queryVector = extractQueryVector(query);
                filtered.stream()
                    .map(object -> object.withScore(cosineSimilarity(queryVector, object.vector)))
                    .sorted(Comparator.comparingDouble(StoredObject::score).reversed())
                    .limit(limit)
                    .forEach(object -> chunksNode.add(toGraphQlNode(object, true)));
            } else {
                filtered.stream()
                    .sorted(Comparator.comparingInt(object -> object.chunkIndex))
                    .limit(limit)
                    .forEach(object -> chunksNode.add(toGraphQlNode(object, false)));
            }

            ObjectNode response = objectMapper.createObjectNode();
            response.putObject("data").putObject("Get").set("DocumentChunk", chunksNode);
            byte[] payload = objectMapper.writeValueAsBytes(response);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        }

        private JsonNode toGraphQlNode(StoredObject object, boolean includeDistance) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("documentId", object.documentId);
            node.put("chunkIndex", object.chunkIndex);
            node.put("textContent", object.textContent);
            node.put("fileName", object.fileName);
            ObjectNode additional = node.putObject("_additional");
            additional.put("id", object.id);
            if (includeDistance) {
                additional.put("distance", Math.max(0.0d, 1.0d - object.score));
            }
            return node;
        }

        private List<String> extractDocumentIds(String query) {
            Matcher matcher = VALUE_TEXT_PATTERN.matcher(query);
            List<String> values = new ArrayList<>();
            while (matcher.find()) {
                values.add(matcher.group(1));
            }
            return values;
        }

        private int extractLimit(String query) {
            Matcher matcher = LIMIT_PATTERN.matcher(query);
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : 10;
        }

        private double[] extractQueryVector(String query) {
            Matcher matcher = VECTOR_PATTERN.matcher(query);
            if (!matcher.find()) {
                return new double[0];
            }
            String[] values = matcher.group(1).split(",");
            double[] vector = new double[values.length];
            for (int index = 0; index < values.length; index++) {
                vector[index] = Double.parseDouble(values[index].trim());
            }
            return vector;
        }

        private double[] toVector(JsonNode vectorNode) {
            double[] vector = new double[vectorNode.size()];
            for (int index = 0; index < vectorNode.size(); index++) {
                vector[index] = vectorNode.get(index).asDouble();
            }
            return vector;
        }

        private double cosineSimilarity(double[] left, double[] right) {
            double dotProduct = 0.0d;
            int dimensions = Math.min(left.length, right.length);
            for (int index = 0; index < dimensions; index++) {
                dotProduct += left[index] * right[index];
            }
            return dotProduct;
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private record StoredObject(String id,
                                String documentId,
                                String fileName,
                                String textContent,
                                int chunkIndex,
                                double[] vector,
                                double score) {
        private StoredObject(String id, String documentId, String fileName, String textContent, int chunkIndex, double[] vector) {
            this(id, documentId, fileName, textContent, chunkIndex, vector, 0.0d);
        }

        private StoredObject withScore(double score) {
            return new StoredObject(id, documentId, fileName, textContent, chunkIndex, vector, score);
        }
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

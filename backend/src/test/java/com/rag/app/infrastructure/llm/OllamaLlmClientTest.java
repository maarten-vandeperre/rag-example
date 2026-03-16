package com.rag.app.infrastructure.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.app.usecases.models.DocumentChunk;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaLlmClientTest {

    @Test
    void shouldGenerateAnswerFromOllamaResponse() throws Exception {
        try (StubOllamaServer server = StubOllamaServer.responding(200, """
            {"response":"Uploads are handled by the Ollama-backed client."}
            """)) {
            OllamaLlmClient client = new OllamaLlmClient(
                new ObjectMapper(),
                server.baseUrl(),
                "tinyllama",
                Duration.ofSeconds(2),
                128,
                0.1d,
                0,
                Duration.ZERO
            );

            String answer = client.generate("Prompt", "How do uploads work?", List.of(sampleChunk()));

            assertEquals("Uploads are handled by the Ollama-backed client.", answer);
            assertTrue(server.lastRequestBody.contains("\"model\":\"tinyllama\""));
            assertTrue(server.lastRequestBody.contains("\"num_predict\":128"));
        }
    }

    @Test
    void shouldSurfaceOllamaErrors() throws Exception {
        try (StubOllamaServer server = StubOllamaServer.responding(500, """
            {"error":"model unavailable"}
            """)) {
            OllamaLlmClient client = new OllamaLlmClient(
                new ObjectMapper(),
                server.baseUrl(),
                "tinyllama",
                Duration.ofSeconds(2),
                128,
                0.1d,
                0,
                Duration.ZERO
            );

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> client.generate("Prompt", "How do uploads work?", List.of(sampleChunk())));

            assertEquals("Ollama request failed with status 500", exception.getMessage());
        }
    }

    private static DocumentChunk sampleChunk() {
        return new DocumentChunk("chunk-1", UUID.randomUUID(), "guide.pdf", 0, "chunk-1", "Upload guidance", new double[0], 0.9d);
    }

    private static final class StubOllamaServer implements AutoCloseable {
        private final HttpServer server;
        private final int port;
        private volatile String lastRequestBody = "";

        private StubOllamaServer(int statusCode, String responseBody) throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            port = server.getAddress().getPort();
            server.createContext("/api/generate", exchange -> handle(exchange, statusCode, responseBody));
            server.start();
        }

        static StubOllamaServer responding(int statusCode, String responseBody) throws IOException {
            return new StubOllamaServer(statusCode, responseBody);
        }

        String baseUrl() {
            return "http://127.0.0.1:" + port;
        }

        private void handle(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}

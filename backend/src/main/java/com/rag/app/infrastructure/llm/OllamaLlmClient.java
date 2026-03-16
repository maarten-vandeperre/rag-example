package com.rag.app.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.app.usecases.models.DocumentChunk;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OllamaLlmClient implements LlmClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI generateUri;
    private final String model;
    private final Duration requestTimeout;
    private final int maxTokens;
    private final double temperature;
    private final int retryCount;
    private final Duration retryDelay;

    public OllamaLlmClient(ObjectMapper objectMapper,
                           String baseUrl,
                           String model,
                           Duration requestTimeout,
                           int maxTokens,
                           double temperature,
                           int retryCount,
                           Duration retryDelay) {
        this(
            HttpClient.newBuilder()
                .connectTimeout(Objects.requireNonNull(requestTimeout, "requestTimeout must not be null"))
                .build(),
            objectMapper,
            URI.create(normalizeBaseUrl(baseUrl) + "/api/generate"),
            model,
            requestTimeout,
            maxTokens,
            temperature,
            retryCount,
            retryDelay
        );
    }

    OllamaLlmClient(HttpClient httpClient,
                    ObjectMapper objectMapper,
                    URI generateUri,
                    String model,
                    Duration requestTimeout,
                    int maxTokens,
                    double temperature,
                    int retryCount,
                    Duration retryDelay) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.generateUri = Objects.requireNonNull(generateUri, "generateUri must not be null");
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be null or blank");
        }
        this.model = model.trim();
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
        this.retryCount = retryCount;
        this.retryDelay = Objects.requireNonNull(retryDelay, "retryDelay must not be null");
    }

    @Override
    public String generate(String prompt, String question, List<DocumentChunk> context) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be null or empty");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
        if (context == null || context.isEmpty()) {
            throw new IllegalArgumentException("context must not be null or empty");
        }

        String requestBody = serializeRequest(prompt);
        IllegalStateException lastFailure = null;

        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(generateUri)
                    .header("Content-Type", "application/json")
                    .timeout(requestTimeout)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 500) {
                    throw new IllegalStateException("Ollama request failed with status " + response.statusCode());
                }
                if (response.statusCode() >= 400) {
                    throw new IllegalStateException("Ollama request failed with status " + response.statusCode());
                }

                OllamaGenerateResponse generateResponse = objectMapper.readValue(response.body(), OllamaGenerateResponse.class);
                if (generateResponse.error != null && !generateResponse.error.isBlank()) {
                    throw new IllegalStateException(generateResponse.error);
                }
                if (generateResponse.response == null || generateResponse.response.isBlank()) {
                    throw new IllegalStateException("Ollama returned an empty response");
                }

                return generateResponse.response.strip();
            } catch (IOException exception) {
                lastFailure = new IllegalStateException("Failed to parse Ollama response", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ollama request was interrupted", exception);
            } catch (IllegalStateException exception) {
                lastFailure = exception;
                if (!shouldRetry(attempt, exception)) {
                    throw exception;
                }
            }

            sleepBeforeRetry(attempt);
        }

        throw lastFailure == null ? new IllegalStateException("Ollama request failed") : lastFailure;
    }

    private String serializeRequest(String prompt) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                    "temperature", temperature,
                    "num_predict", maxTokens
                )
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build Ollama request", exception);
        }
    }

    private boolean shouldRetry(int attempt, IllegalStateException exception) {
        return attempt < retryCount && exception.getMessage() != null && exception.getMessage().contains("status 5");
    }

    private void sleepBeforeRetry(int attempt) {
        if (attempt >= retryCount || retryDelay.isZero() || retryDelay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama retry wait was interrupted", exception);
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be null or blank");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class OllamaGenerateResponse {
        public String response;
        public String error;
    }
}

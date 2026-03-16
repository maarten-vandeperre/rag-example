package com.rag.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.app.infrastructure.llm.HeuristicLlmClient;
import com.rag.app.infrastructure.llm.LlmClient;
import com.rag.app.infrastructure.llm.OllamaLlmClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class LlmConfiguration {
    @ConfigProperty(name = "app.llm.provider", defaultValue = "heuristic")
    String llmProvider;

    @ConfigProperty(name = "app.llm.url", defaultValue = "http://localhost:11434")
    String llmUrl;

    @ConfigProperty(name = "app.llm.model", defaultValue = "tinyllama")
    String llmModel;

    @ConfigProperty(name = "app.llm.timeout", defaultValue = "20000")
    int llmTimeoutMs;

    @ConfigProperty(name = "app.llm.max-tokens", defaultValue = "2048")
    int maxTokens;

    @ConfigProperty(name = "app.llm.temperature", defaultValue = "0.1")
    double temperature;

    @ConfigProperty(name = "app.llm.retry-count", defaultValue = "2")
    int retryCount;

    @ConfigProperty(name = "app.llm.retry-delay-ms", defaultValue = "1000")
    int retryDelayMs;

    @Produces
    @ApplicationScoped
    public LlmClient llmClient(ObjectMapper objectMapper) {
        if ("ollama".equalsIgnoreCase(llmProvider)) {
            return new OllamaLlmClient(
                objectMapper,
                llmUrl,
                llmModel,
                Duration.ofMillis(llmTimeoutMs),
                maxTokens,
                temperature,
                retryCount,
                Duration.ofMillis(retryDelayMs)
            );
        }
        return new HeuristicLlmClient();
    }
}

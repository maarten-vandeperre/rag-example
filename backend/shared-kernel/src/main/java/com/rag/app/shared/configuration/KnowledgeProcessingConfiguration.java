package com.rag.app.shared.configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.time.Duration;

public final class KnowledgeProcessingConfiguration {
    private final Map<String, Boolean> enabledByDocumentType;
    private final Map<String, Map<String, Object>> extractionOptionsByDocumentType;
    private final String defaultGraphName;
    private final Duration processingTimeout;
    private final int maxRetryAttempts;

    public KnowledgeProcessingConfiguration() {
        this(defaultEnabledByDocumentType(), defaultExtractionOptionsByDocumentType(), "main-knowledge-graph", Duration.ofSeconds(10), 1);
    }

    public KnowledgeProcessingConfiguration(Map<String, Boolean> enabledByDocumentType,
                                            Map<String, Map<String, Object>> extractionOptionsByDocumentType,
                                            String defaultGraphName) {
        this(enabledByDocumentType, extractionOptionsByDocumentType, defaultGraphName, Duration.ofSeconds(10), 1);
    }

    public KnowledgeProcessingConfiguration(Map<String, Boolean> enabledByDocumentType,
                                            Map<String, Map<String, Object>> extractionOptionsByDocumentType,
                                            String defaultGraphName,
                                            Duration processingTimeout,
                                            int maxRetryAttempts) {
        this.enabledByDocumentType = normalizeEnabledByDocumentType(enabledByDocumentType);
        this.extractionOptionsByDocumentType = normalizeExtractionOptionsByDocumentType(extractionOptionsByDocumentType);
        if (defaultGraphName == null || defaultGraphName.isBlank()) {
            throw new IllegalArgumentException("defaultGraphName cannot be null or blank");
        }
        Objects.requireNonNull(processingTimeout, "processingTimeout cannot be null");
        if (processingTimeout.isZero() || processingTimeout.isNegative()) {
            throw new IllegalArgumentException("processingTimeout must be positive");
        }
        if (maxRetryAttempts < 0) {
            throw new IllegalArgumentException("maxRetryAttempts cannot be negative");
        }
        this.defaultGraphName = defaultGraphName.trim();
        this.processingTimeout = processingTimeout;
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public boolean isEnabledForDocumentType(String documentType) {
        return enabledByDocumentType.getOrDefault(normalizeDocumentType(documentType), false);
    }

    public Map<String, Object> getExtractionOptionsFor(String documentType) {
        return extractionOptionsByDocumentType.getOrDefault(normalizeDocumentType(documentType), defaultExtractionOptions());
    }

    public String defaultGraphName() {
        return defaultGraphName;
    }

    public Duration processingTimeout() {
        return processingTimeout;
    }

    public int maxRetryAttempts() {
        return maxRetryAttempts;
    }

    public boolean shouldRetry(int attemptNumber, List<String> errors) {
        Objects.requireNonNull(errors, "errors cannot be null");
        return attemptNumber <= maxRetryAttempts && errors.stream().anyMatch(this::isTransientError);
    }

    private static Map<String, Boolean> defaultEnabledByDocumentType() {
        Map<String, Boolean> enabled = new LinkedHashMap<>();
        enabled.put("PDF", true);
        enabled.put("MARKDOWN", true);
        enabled.put("PLAIN_TEXT", true);
        return enabled;
    }

    private static Map<String, Map<String, Object>> defaultExtractionOptionsByDocumentType() {
        Map<String, Map<String, Object>> options = new LinkedHashMap<>();
        options.put("PDF", Map.of(
            "extract_entities", true,
            "extract_relationships", true,
            "min_confidence", 0.7d,
            "max_entities_per_page", 50
        ));
        options.put("MARKDOWN", Map.of(
            "extract_entities", true,
            "extract_relationships", true,
            "min_confidence", 0.6d,
            "chunk_size", 1000
        ));
        options.put("PLAIN_TEXT", Map.of(
            "extract_entities", true,
            "extract_relationships", false,
            "min_confidence", 0.5d,
            "chunk_size", 1200
        ));
        return options;
    }

    private static Map<String, Object> defaultExtractionOptions() {
        return Map.of(
            "extract_entities", true,
            "extract_relationships", false,
            "min_confidence", 0.5d
        );
    }

    private static Map<String, Boolean> normalizeEnabledByDocumentType(Map<String, Boolean> enabledByDocumentType) {
        Objects.requireNonNull(enabledByDocumentType, "enabledByDocumentType cannot be null");
        Map<String, Boolean> normalized = new LinkedHashMap<>();
        enabledByDocumentType.forEach((documentType, enabled) -> normalized.put(normalizeDocumentType(documentType), Boolean.TRUE.equals(enabled)));
        return Map.copyOf(normalized);
    }

    private static Map<String, Map<String, Object>> normalizeExtractionOptionsByDocumentType(Map<String, Map<String, Object>> extractionOptionsByDocumentType) {
        Objects.requireNonNull(extractionOptionsByDocumentType, "extractionOptionsByDocumentType cannot be null");
        Map<String, Map<String, Object>> normalized = new LinkedHashMap<>();
        extractionOptionsByDocumentType.forEach((documentType, options) -> normalized.put(
            normalizeDocumentType(documentType),
            Map.copyOf(Objects.requireNonNull(options, "options cannot be null"))
        ));
        return Map.copyOf(normalized);
    }

    private static String normalizeDocumentType(String documentType) {
        if (documentType == null || documentType.isBlank()) {
            throw new IllegalArgumentException("documentType cannot be null or blank");
        }
        return documentType.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isTransientError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return false;
        }
        String normalized = errorMessage.toLowerCase(Locale.ROOT);
        return normalized.contains("timeout")
            || normalized.contains("temporar")
            || normalized.contains("unavailable")
            || normalized.contains("connection")
            || normalized.contains("retry");
    }
}

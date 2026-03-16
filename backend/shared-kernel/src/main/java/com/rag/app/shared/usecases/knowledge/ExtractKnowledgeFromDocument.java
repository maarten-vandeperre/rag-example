package com.rag.app.shared.usecases.knowledge;

import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractionMetadata;
import com.rag.app.shared.interfaces.UseCase;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityResult;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityValidator;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionException;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionService;
import com.rag.app.shared.interfaces.knowledge.UnsupportedDocumentFormatException;
import com.rag.app.shared.usecases.knowledge.models.ExtractKnowledgeInput;
import com.rag.app.shared.usecases.knowledge.models.ExtractKnowledgeOutput;
import com.rag.app.shared.usecases.knowledge.models.KnowledgeExtractionStatus;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ExtractKnowledgeFromDocument implements UseCase<ExtractKnowledgeInput, ExtractKnowledgeOutput> {
    private static final String EMPTY_EXTRACTION_METHOD = "not_executed";

    private final KnowledgeExtractionService knowledgeExtractionService;
    private final DocumentQualityValidator documentQualityValidator;
    private final Clock clock;

    public ExtractKnowledgeFromDocument(KnowledgeExtractionService knowledgeExtractionService,
                                        DocumentQualityValidator documentQualityValidator,
                                        Clock clock) {
        this.knowledgeExtractionService = Objects.requireNonNull(knowledgeExtractionService, "knowledgeExtractionService cannot be null");
        this.documentQualityValidator = Objects.requireNonNull(documentQualityValidator, "documentQualityValidator cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public ExtractKnowledgeOutput execute(ExtractKnowledgeInput input) {
        Objects.requireNonNull(input, "input cannot be null");

        Instant startedAt = clock.instant();
        List<String> warnings = new ArrayList<>();

        try {
            DocumentQualityResult qualityResult = documentQualityValidator.validateForKnowledgeExtraction(
                input.documentContent(),
                input.documentType()
            );
            warnings.addAll(qualityResult.warnings());

            if (!qualityResult.sufficientForExtraction()) {
                warnings.addAll(qualityResult.issues());
                return createOutput(input, startedAt, KnowledgeExtractionStatus.INSUFFICIENT_CONTENT, warnings, List.of(), null);
            }

            if (!knowledgeExtractionService.supportsDocumentType(input.documentType())) {
                return createOutput(
                    input,
                    startedAt,
                    KnowledgeExtractionStatus.UNSUPPORTED_FORMAT,
                    warnings,
                    List.of("Unsupported document type: " + input.documentType()),
                    null
                );
            }

            ExtractedKnowledge extractedKnowledge = knowledgeExtractionService.extractKnowledge(
                input.documentContent(),
                input.documentTitle(),
                input.documentType(),
                mergedExtractionOptions(input)
            );

            warnings.addAll(extractedKnowledge.metadata().warnings());
            return createOutput(input, startedAt, determineStatus(extractedKnowledge, warnings), warnings, List.of(), extractedKnowledge);
        } catch (UnsupportedDocumentFormatException exception) {
            return createOutput(input, startedAt, KnowledgeExtractionStatus.UNSUPPORTED_FORMAT, warnings, List.of(exception.getMessage()), null);
        } catch (KnowledgeExtractionException exception) {
            return createOutput(input, startedAt, KnowledgeExtractionStatus.PROCESSING_ERROR, warnings, List.of(exception.getMessage()), null);
        } catch (RuntimeException exception) {
            return createOutput(
                input,
                startedAt,
                KnowledgeExtractionStatus.PROCESSING_ERROR,
                warnings,
                List.of("Unexpected error during knowledge extraction: " + exception.getMessage()),
                null
            );
        }
    }

    private Map<String, Object> mergedExtractionOptions(ExtractKnowledgeInput input) {
        var mergedOptions = new java.util.LinkedHashMap<>(knowledgeExtractionService.getDefaultExtractionOptions());
        mergedOptions.putAll(input.extractionOptions());
        return Map.copyOf(mergedOptions);
    }

    private KnowledgeExtractionStatus determineStatus(ExtractedKnowledge extractedKnowledge, List<String> warnings) {
        if (extractedKnowledge.isEmpty()) {
            return KnowledgeExtractionStatus.INSUFFICIENT_CONTENT;
        }
        if (!warnings.isEmpty()) {
            return KnowledgeExtractionStatus.PARTIAL_SUCCESS;
        }
        return KnowledgeExtractionStatus.SUCCESS;
    }

    private ExtractKnowledgeOutput createOutput(ExtractKnowledgeInput input,
                                                Instant startedAt,
                                                KnowledgeExtractionStatus status,
                                                List<String> warnings,
                                                List<String> errors,
                                                ExtractedKnowledge extractedKnowledge) {
        Instant completedAt = clock.instant();
        Duration processingTime = Duration.between(startedAt, completedAt);
        ExtractedKnowledge outputKnowledge = extractedKnowledge == null
            ? emptyKnowledge(input, completedAt, processingTime, warnings)
            : extractedKnowledge;
        return new ExtractKnowledgeOutput(outputKnowledge, status, warnings, errors, processingTime);
    }

    private ExtractedKnowledge emptyKnowledge(ExtractKnowledgeInput input,
                                              Instant completedAt,
                                              Duration processingTime,
                                              List<String> warnings) {
        return new ExtractedKnowledge(
            List.of(),
            List.of(),
            new DocumentReference(resolveDocumentId(input.documentId()), input.documentTitle(), null, 0.0d),
            new ExtractionMetadata(EMPTY_EXTRACTION_METHOD, completedAt, processingTime, Map.of(), warnings)
        );
    }

    private UUID resolveDocumentId(String documentId) {
        try {
            return UUID.fromString(documentId);
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(documentId.getBytes(StandardCharsets.UTF_8));
        }
    }
}

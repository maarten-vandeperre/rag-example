package com.rag.app.document.usecases;

import com.rag.app.document.domain.entities.Document;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.domain.valueobjects.KnowledgeProcessingStatus;
import com.rag.app.document.interfaces.DocumentContentExtractor;
import com.rag.app.document.interfaces.DocumentRepository;
import com.rag.app.document.interfaces.DocumentStorage;
import com.rag.app.document.interfaces.DocumentVectorStore;
import com.rag.app.document.usecases.models.KnowledgeProcessingResult;
import com.rag.app.document.usecases.models.ProcessDocumentInput;
import com.rag.app.document.usecases.models.ProcessDocumentOutput;
import com.rag.app.document.usecases.models.SearchProcessingResult;
import com.rag.app.shared.configuration.KnowledgeProcessingConfiguration;
import com.rag.app.shared.usecases.knowledge.BuildKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.ExtractKnowledgeFromDocument;
import com.rag.app.shared.usecases.knowledge.models.BuildKnowledgeGraphInput;
import com.rag.app.shared.usecases.knowledge.models.ExtractKnowledgeInput;
import com.rag.app.shared.usecases.knowledge.models.ExtractKnowledgeOutput;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class ProcessDocument {
    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final DocumentContentExtractor documentContentExtractor;
    private final DocumentVectorStore documentVectorStore;
    private final ExtractKnowledgeFromDocument extractKnowledgeFromDocument;
    private final BuildKnowledgeGraph buildKnowledgeGraph;
    private final KnowledgeProcessingConfiguration knowledgeProcessingConfiguration;
    private final Clock clock;

    public ProcessDocument(DocumentRepository documentRepository,
                           DocumentStorage documentStorage,
                           DocumentContentExtractor documentContentExtractor,
                           DocumentVectorStore documentVectorStore,
                           ExtractKnowledgeFromDocument extractKnowledgeFromDocument,
                           BuildKnowledgeGraph buildKnowledgeGraph,
                           KnowledgeProcessingConfiguration knowledgeProcessingConfiguration,
                           Clock clock) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.documentStorage = Objects.requireNonNull(documentStorage, "documentStorage must not be null");
        this.documentContentExtractor = Objects.requireNonNull(documentContentExtractor, "documentContentExtractor must not be null");
        this.documentVectorStore = Objects.requireNonNull(documentVectorStore, "documentVectorStore must not be null");
        this.extractKnowledgeFromDocument = Objects.requireNonNull(extractKnowledgeFromDocument, "extractKnowledgeFromDocument must not be null");
        this.buildKnowledgeGraph = Objects.requireNonNull(buildKnowledgeGraph, "buildKnowledgeGraph must not be null");
        this.knowledgeProcessingConfiguration = Objects.requireNonNull(knowledgeProcessingConfiguration, "knowledgeProcessingConfiguration must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ProcessDocumentOutput execute(ProcessDocumentInput input) {
        Objects.requireNonNull(input, "input must not be null");
        if (input.documentId() == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }

        Document document = documentRepository.findById(input.documentId())
            .orElseThrow(() -> new IllegalArgumentException("document must exist"));

        byte[] fileContent = documentStorage.load(input.documentId())
            .orElseThrow(() -> new IllegalArgumentException("fileContent must exist for document"));

        Instant now = Instant.now(clock);
        Document processingDocument = document.withProcessingStarted(now);
        if (knowledgeProcessingConfiguration.isEnabledForDocumentType(document.fileType().name())) {
            processingDocument = processingDocument.withKnowledgeProcessingStarted(now);
        } else {
            processingDocument = processingDocument.withKnowledgeProcessingSkipped(
                KnowledgeProcessingStatus.DISABLED,
                List.of("Knowledge processing disabled for document type: " + document.fileType().name()),
                "Knowledge processing disabled for document type: " + document.fileType().name(),
                now
            );
        }
        documentRepository.save(processingDocument);

        try {
            String extractedText = documentContentExtractor.extractText(fileContent, document.fileType());
            if (extractedText == null || extractedText.isBlank()) {
                Document failedDocument = processingDocument.withStatus(DocumentStatus.FAILED, Instant.now(clock))
                    .withFailureReason("No usable content extracted from document", Instant.now(clock));
                documentRepository.save(failedDocument);
                return new ProcessDocumentOutput(
                    failedDocument.documentId(),
                    DocumentStatus.FAILED,
                    0,
                    "No usable content extracted from document",
                    failedDocument.knowledgeProcessingStatus(),
                    failedDocument.knowledgeProcessingWarnings(),
                    failedDocument.knowledgeProcessingError(),
                    failedDocument.associatedGraphId()
                );
            }

            Document currentDocument = processingDocument;
            CompletableFuture<SearchProcessingResult> searchFuture = CompletableFuture.supplyAsync(
                () -> processForSearch(currentDocument, extractedText)
            );
            CompletableFuture<KnowledgeProcessingResult> knowledgeFuture = CompletableFuture.supplyAsync(
                () -> processForKnowledge(currentDocument, extractedText)
            ).completeOnTimeout(
                KnowledgeProcessingResult.failure(
                    "Knowledge processing timed out after " + knowledgeProcessingConfiguration.processingTimeout().toMillis() + "ms",
                    List.of("Knowledge processing timed out after " + knowledgeProcessingConfiguration.processingTimeout().toMillis() + "ms"),
                    List.of()
                ),
                knowledgeProcessingConfiguration.processingTimeout().toMillis(),
                TimeUnit.MILLISECONDS
            );

            SearchProcessingResult searchResult = searchFuture.join();
            KnowledgeProcessingResult knowledgeResult = knowledgeFuture.join();

            Document finalDocument = updateDocumentWithResults(currentDocument, searchResult, knowledgeResult);
            documentRepository.save(finalDocument);
            return new ProcessDocumentOutput(
                finalDocument.documentId(),
                finalDocument.status(),
                extractedText.length(),
                finalDocument.failureReason(),
                finalDocument.knowledgeProcessingStatus(),
                finalDocument.knowledgeProcessingWarnings(),
                finalDocument.knowledgeProcessingError(),
                finalDocument.associatedGraphId()
            );
        } catch (RuntimeException exception) {
            Document failedDocument = processingDocument.withStatus(DocumentStatus.FAILED, Instant.now(clock))
                .withFailureReason(exception.getMessage(), Instant.now(clock));
            documentRepository.save(failedDocument);
            return new ProcessDocumentOutput(
                failedDocument.documentId(),
                DocumentStatus.FAILED,
                0,
                exception.getMessage(),
                failedDocument.knowledgeProcessingStatus(),
                failedDocument.knowledgeProcessingWarnings(),
                failedDocument.knowledgeProcessingError(),
                failedDocument.associatedGraphId()
            );
        }
    }

    private SearchProcessingResult processForSearch(Document document, String content) {
        try {
            documentVectorStore.storeDocument(document.documentId(), content);
            return SearchProcessingResult.success();
        } catch (RuntimeException exception) {
            return SearchProcessingResult.failure("Search processing failed: " + exception.getMessage());
        }
    }

    private KnowledgeProcessingResult processForKnowledge(Document document, String content) {
        if (!knowledgeProcessingConfiguration.isEnabledForDocumentType(document.fileType().name())) {
            return KnowledgeProcessingResult.disabled("Knowledge processing disabled for document type: " + document.fileType().name());
        }

        for (int attempt = 0; attempt <= knowledgeProcessingConfiguration.maxRetryAttempts(); attempt++) {
            KnowledgeProcessingResult attemptResult = attemptKnowledgeProcessing(document, content);
            if (attemptResult.isSuccessful() || attemptResult.isSkipped() || attemptResult.isDisabled()) {
                return attemptResult;
            }
            if (!knowledgeProcessingConfiguration.shouldRetry(attempt + 1, attemptResult.errors())) {
                return attemptResult;
            }
        }
        return KnowledgeProcessingResult.failure("Knowledge processing failed after retries", List.of("Knowledge processing failed after retries"), List.of());
    }

    private KnowledgeProcessingResult attemptKnowledgeProcessing(Document document, String content) {
        try {
            ExtractKnowledgeOutput extractOutput = extractKnowledgeFromDocument.execute(new ExtractKnowledgeInput(
                document.documentId().toString(),
                content,
                document.fileName(),
                document.fileType().name(),
                knowledgeProcessingConfiguration.getExtractionOptionsFor(document.fileType().name())
            ));

            if (!extractOutput.isSuccessful()) {
                if (extractOutput.status() == com.rag.app.shared.usecases.knowledge.models.KnowledgeExtractionStatus.INSUFFICIENT_CONTENT) {
                    return KnowledgeProcessingResult.skipped("No knowledge extracted from document", extractOutput.warnings());
                }
                return KnowledgeProcessingResult.failure(
                    "Knowledge extraction failed",
                    extractOutput.errors(),
                    extractOutput.warnings()
                );
            }

            if (extractOutput.extractedKnowledge().isEmpty()) {
                return KnowledgeProcessingResult.skipped("No knowledge extracted from document", extractOutput.warnings());
            }

            var buildOutput = buildKnowledgeGraph.execute(new BuildKnowledgeGraphInput(
                knowledgeProcessingConfiguration.defaultGraphName(),
                extractOutput.extractedKnowledge(),
                true
            ));

            if (!buildOutput.success()) {
                return KnowledgeProcessingResult.failure(
                    "Knowledge graph building failed: " + buildOutput.errorMessage(),
                    List.of(buildOutput.errorMessage()),
                    extractOutput.warnings()
                );
            }

            return KnowledgeProcessingResult.success(buildOutput.graphId(), extractOutput.warnings());
        } catch (RuntimeException exception) {
            return KnowledgeProcessingResult.failure(
                "Knowledge processing failed: " + exception.getMessage(),
                List.of(exception.getMessage()),
                List.of()
            );
        }
    }

    private Document updateDocumentWithResults(Document document,
                                               SearchProcessingResult searchResult,
                                               KnowledgeProcessingResult knowledgeResult) {
        Instant completedAt = Instant.now(clock);
        Document updatedDocument = searchResult.isSuccessful()
            ? document.withStatus(DocumentStatus.READY, completedAt).withFailureReason(null, completedAt)
            : document.withStatus(DocumentStatus.FAILED, completedAt).withFailureReason(searchResult.errorMessage(), completedAt);

        if (knowledgeResult.isSuccessful()) {
            return updatedDocument.withKnowledgeProcessingCompleted(knowledgeResult.graphId(), knowledgeResult.warnings(), completedAt);
        }
        if (knowledgeResult.isDisabled()) {
            return updatedDocument.withKnowledgeProcessingSkipped(KnowledgeProcessingStatus.DISABLED, knowledgeResult.warnings(), knowledgeResult.errorMessage(), completedAt);
        }
        if (knowledgeResult.isSkipped()) {
            return updatedDocument.withKnowledgeProcessingSkipped(KnowledgeProcessingStatus.SKIPPED, knowledgeResult.warnings(), knowledgeResult.errorMessage(), completedAt);
        }
        return updatedDocument.withKnowledgeProcessingFailed(knowledgeResult.errorMessage(), knowledgeResult.warnings(), completedAt);
    }
}

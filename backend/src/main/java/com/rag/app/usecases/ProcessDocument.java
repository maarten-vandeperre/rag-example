package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.shared.configuration.KnowledgeProcessingConfiguration;
import com.rag.app.shared.usecases.knowledge.BuildKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.ExtractKnowledgeFromDocument;
import com.rag.app.shared.usecases.knowledge.models.BuildKnowledgeGraphInput;
import com.rag.app.shared.usecases.knowledge.models.BuildKnowledgeGraphOutput;
import com.rag.app.shared.usecases.knowledge.models.ExtractKnowledgeInput;
import com.rag.app.shared.usecases.knowledge.models.ExtractKnowledgeOutput;
import com.rag.app.usecases.interfaces.DocumentContentExtractor;
import com.rag.app.usecases.interfaces.VectorStore;
import com.rag.app.usecases.models.ProcessDocumentInput;
import com.rag.app.usecases.models.ProcessDocumentOutput;
import com.rag.app.usecases.repositories.DocumentRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import org.jboss.logging.Logger;

@ApplicationScoped
public final class ProcessDocument {
    private static final Logger LOG = Logger.getLogger(ProcessDocument.class);

    private final DocumentRepository documentRepository;
    private final DocumentContentExtractor documentContentExtractor;
    private final VectorStore vectorStore;
    private final ExtractKnowledgeFromDocument extractKnowledgeFromDocument;
    private final BuildKnowledgeGraph buildKnowledgeGraph;
    private final KnowledgeProcessingConfiguration knowledgeProcessingConfiguration;

    public ProcessDocument(DocumentRepository documentRepository,
                           DocumentContentExtractor documentContentExtractor,
                           VectorStore vectorStore) {
        this(documentRepository, documentContentExtractor, vectorStore, null, null, null);
    }

    @Inject
    public ProcessDocument(DocumentRepository documentRepository,
                           DocumentContentExtractor documentContentExtractor,
                           VectorStore vectorStore,
                           ExtractKnowledgeFromDocument extractKnowledgeFromDocument,
                           BuildKnowledgeGraph buildKnowledgeGraph,
                           KnowledgeProcessingConfiguration knowledgeProcessingConfiguration) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.documentContentExtractor = Objects.requireNonNull(documentContentExtractor, "documentContentExtractor must not be null");
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
        this.extractKnowledgeFromDocument = extractKnowledgeFromDocument;
        this.buildKnowledgeGraph = buildKnowledgeGraph;
        this.knowledgeProcessingConfiguration = knowledgeProcessingConfiguration;
    }

    public ProcessDocumentOutput execute(ProcessDocumentInput input) {
        Objects.requireNonNull(input, "input must not be null");

        if (input.documentId() == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        if (input.fileContent() == null || input.fileContent().length == 0) {
            throw new IllegalArgumentException("fileContent must not be null or empty");
        }

        Document document = documentRepository.findById(input.documentId())
            .orElseThrow(() -> new IllegalArgumentException("document must exist"));

        document = documentRepository.save(document.withStatus(DocumentStatus.PROCESSING));

        try {
            String extractedText = documentContentExtractor.extractText(input.fileContent(), document.fileType());

            if (extractedText == null || extractedText.isBlank()) {
                documentRepository.save(document.withStatus(DocumentStatus.FAILED));
                return new ProcessDocumentOutput(document.documentId(), DocumentStatus.FAILED, 0, "No usable content extracted from document");
            }

            vectorStore.storeDocumentVectors(document.documentId().toString(), extractedText);
            processKnowledgeGraph(document, extractedText);
            documentRepository.save(document.withStatus(DocumentStatus.READY));
            return new ProcessDocumentOutput(document.documentId(), DocumentStatus.READY, extractedText.length(), null);
        } catch (RuntimeException exception) {
            documentRepository.save(document.withStatus(DocumentStatus.FAILED));
            return new ProcessDocumentOutput(document.documentId(), DocumentStatus.FAILED, 0, exception.getMessage());
        }
    }

    private void processKnowledgeGraph(Document document, String extractedText) {
        if (extractKnowledgeFromDocument == null || buildKnowledgeGraph == null || knowledgeProcessingConfiguration == null) {
            return;
        }

        String documentType = document.fileType().name();
        if (!knowledgeProcessingConfiguration.isEnabledForDocumentType(documentType)) {
            return;
        }

        try {
            ExtractKnowledgeOutput extractionOutput = extractKnowledgeFromDocument.execute(new ExtractKnowledgeInput(
                document.documentId().toString(),
                extractedText,
                document.fileName(),
                documentType,
                knowledgeProcessingConfiguration.getExtractionOptionsFor(documentType)
            ));

            if (!extractionOutput.isSuccessful() || extractionOutput.extractedKnowledge().isEmpty()) {
                LOG.warnf("Knowledge extraction skipped or incomplete for document %s with status %s and errors %s",
                    document.documentId(), extractionOutput.status(), extractionOutput.errors());
                return;
            }

            BuildKnowledgeGraphOutput graphOutput = buildKnowledgeGraph.execute(new BuildKnowledgeGraphInput(
                graphNameFor(document),
                extractionOutput.extractedKnowledge(),
                true
            ));

            if (!graphOutput.success()) {
                LOG.warnf("Knowledge graph creation failed for document %s: %s", document.documentId(), graphOutput.errorMessage());
                return;
            }

            LOG.infof("Created knowledge graph %s for document %s with %d nodes and %d relationships",
                graphOutput.graphId(), document.documentId(), graphOutput.totalNodes(), graphOutput.totalRelationships());
        } catch (RuntimeException exception) {
            LOG.warnf(exception, "Knowledge graph processing failed for document %s but document search processing will continue", document.documentId());
        }
    }

    private String graphNameFor(Document document) {
        return knowledgeProcessingConfiguration.defaultGraphName() + "-" + document.documentId();
    }
}

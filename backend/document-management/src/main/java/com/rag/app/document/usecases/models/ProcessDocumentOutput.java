package com.rag.app.document.usecases.models;

import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.domain.valueobjects.KnowledgeProcessingStatus;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;

import java.util.List;
import java.util.UUID;

public record ProcessDocumentOutput(UUID documentId,
                                    DocumentStatus finalStatus,
                                    int extractedTextLength,
                                    String errorMessage,
                                    KnowledgeProcessingStatus knowledgeProcessingStatus,
                                    List<String> knowledgeProcessingWarnings,
                                    String knowledgeProcessingError,
                                    GraphId associatedGraphId) {
}

package com.rag.app.shared.interfaces.knowledge;

import com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge;

import java.util.List;
import java.util.Map;

public interface KnowledgeExtractionService {
    ExtractedKnowledge extractKnowledge(String documentContent,
                                        String documentTitle,
                                        String documentType,
                                        Map<String, Object> extractionOptions) throws KnowledgeExtractionException;

    boolean supportsDocumentType(String documentType);

    List<String> getSupportedExtractionTypes();

    Map<String, Object> getDefaultExtractionOptions();
}

package com.rag.app.integration.api.dto.knowledge;

import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;

public record KnowledgeDocumentReferenceDto(String documentId,
                                            String documentName,
                                            String sectionReference,
                                            double relevanceScore) {
    public static KnowledgeDocumentReferenceDto fromDomain(DocumentReference documentReference) {
        if (documentReference == null) {
            return null;
        }
        return new KnowledgeDocumentReferenceDto(
            documentReference.documentId().toString(),
            documentReference.documentName(),
            documentReference.sectionReference(),
            documentReference.relevanceScore()
        );
    }
}

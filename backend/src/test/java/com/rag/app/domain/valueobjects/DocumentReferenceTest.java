package com.rag.app.domain.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentReferenceTest {

    @Test
    void shouldCreateDocumentReferenceWhenRequiredFieldsAreValid() {
        UUID documentId = UUID.randomUUID();

        DocumentReference reference = new DocumentReference(documentId, "handbook.md", "section-2", 0.87d);

        assertEquals(documentId, reference.documentId());
        assertEquals("handbook.md", reference.documentName());
        assertEquals("section-2", reference.paragraphReference());
        assertEquals(0.87d, reference.relevanceScore());
    }

    @Test
    void shouldRejectDocumentReferenceWhenDocumentIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new DocumentReference(null, "handbook.md", "section-2", 0.87d));

        assertEquals("documentId must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectDocumentReferenceWhenDocumentNameIsNullOrBlank() {
        IllegalArgumentException nullNameException = assertThrows(IllegalArgumentException.class,
            () -> new DocumentReference(UUID.randomUUID(), null, "section-2", 0.87d));
        IllegalArgumentException blankNameException = assertThrows(IllegalArgumentException.class,
            () -> new DocumentReference(UUID.randomUUID(), "   ", "section-2", 0.87d));

        assertEquals("documentName must not be null or empty", nullNameException.getMessage());
        assertEquals("documentName must not be null or empty", blankNameException.getMessage());
    }
}

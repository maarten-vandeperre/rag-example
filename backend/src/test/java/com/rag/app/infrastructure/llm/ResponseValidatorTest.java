package com.rag.app.infrastructure.llm;

import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.usecases.models.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResponseValidatorTest {

    @Test
    void shouldAcceptGroundedAnswerWithReferences() {
        ResponseValidator validator = new ResponseValidator();
        DocumentChunk chunk = chunk("guide.pdf", "Uploads are processed asynchronously after the file is stored.");
        DocumentReference reference = new DocumentReference(chunk.documentId(), chunk.documentName(), chunk.paragraphReference(), chunk.relevanceScore());

        String answer = validator.validate("Uploads are processed asynchronously after the file is stored.",
            "How are uploads processed?", List.of(chunk), List.of(reference));

        assertEquals("Uploads are processed asynchronously after the file is stored.", answer);
    }

    @Test
    void shouldRejectHallucinatedAnswer() {
        ResponseValidator validator = new ResponseValidator();
        DocumentChunk chunk = chunk("guide.pdf", "Uploads are processed asynchronously after the file is stored.");
        DocumentReference reference = new DocumentReference(chunk.documentId(), chunk.documentName(), chunk.paragraphReference(), chunk.relevanceScore());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> validator.validate("The system uses satellite replication across continents.",
                "How are uploads processed?", List.of(chunk), List.of(reference)));

        assertEquals("Generated answer must be grounded in the provided context", exception.getMessage());
    }

    private DocumentChunk chunk(String documentName, String text) {
        return new DocumentChunk("chunk-1", UUID.randomUUID(), documentName, 0, "chunk-1", text, new double[0], 0.91d);
    }
}

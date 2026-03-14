package com.rag.app.infrastructure.llm;

import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.usecases.interfaces.AnswerGenerator;
import com.rag.app.usecases.models.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerGeneratorImplTest {

    @Test
    void shouldGenerateGroundedAnswerAndReferences() {
        AnswerGenerator generator = new AnswerGeneratorImpl(new PromptTemplate(), new HeuristicLlmClient(), new ResponseValidator());
        DocumentChunk first = chunk("guide.pdf", "chunk-1", "Uploads are processed asynchronously after the file is stored.", 0.98d);
        DocumentChunk second = chunk("faq.md", "chunk-4", "Chat answers cite the most relevant source sections for the user.", 0.92d);

        AnswerGenerator.GeneratedAnswer answer = generator.generateAnswer(
            "How do uploads and chat references work?",
            List.of(first, second)
        );

        assertTrue(answer.answer().contains("guide.pdf"));
        assertTrue(answer.answer().contains("faq.md"));
        assertEquals(2, answer.documentReferences().size());
        assertEquals("guide.pdf", answer.documentReferences().get(0).documentName());
    }

    @Test
    void shouldReturnNoInformationMessageWhenContextDoesNotMatchQuestion() {
        AnswerGenerator generator = new AnswerGeneratorImpl(new PromptTemplate(), new HeuristicLlmClient(), new ResponseValidator());

        AnswerGenerator.GeneratedAnswer answer = generator.generateAnswer(
            "What is the telescope orbit?",
            List.of(chunk("guide.pdf", "chunk-1", "Uploads are processed asynchronously after the file is stored.", 0.98d))
        );

        assertEquals(ResponseValidator.NO_INFORMATION_MESSAGE, answer.answer());
        assertTrue(answer.documentReferences().isEmpty());
    }

    @Test
    void shouldThrowHelpfulErrorWhenClientFails() {
        AnswerGenerator generator = new AnswerGeneratorImpl(
            new PromptTemplate(),
            (prompt, question, context) -> {
                throw new IllegalStateException("timeout");
            },
            new ResponseValidator()
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> generator.generateAnswer("How do uploads work?", List.of(chunk("guide.pdf", "chunk-1", "Uploads are processed asynchronously.", 0.98d))));

        assertEquals("Failed to generate answer", exception.getMessage());
    }

    @Test
    void shouldHandleMultipleSourcesInReferenceExtraction() {
        AnswerGeneratorImpl generator = new AnswerGeneratorImpl(new PromptTemplate(),
            (prompt, question, context) -> "Uploads are processed asynchronously. Chat answers cite source sections. (Source: guide.pdf chunk-1) (Source: faq.md chunk-4)",
            new ResponseValidator());

        AnswerGenerator.GeneratedAnswer answer = generator.generateAnswer(
            "How do uploads and chat references work?",
            List.of(
                chunk("guide.pdf", "chunk-1", "Uploads are processed asynchronously after the file is stored.", 0.98d),
                chunk("faq.md", "chunk-4", "Chat answers cite the most relevant source sections for the user.", 0.92d)
            )
        );

        List<DocumentReference> references = answer.documentReferences();
        assertEquals(2, references.size());
        assertEquals("guide.pdf", references.get(0).documentName());
        assertEquals("faq.md", references.get(1).documentName());
    }

    private static DocumentChunk chunk(String documentName, String reference, String text, double score) {
        return new DocumentChunk(reference + "-id", UUID.randomUUID(), documentName, 0, reference, text, new double[0], score);
    }
}

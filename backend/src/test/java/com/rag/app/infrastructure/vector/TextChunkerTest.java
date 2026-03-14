package com.rag.app.infrastructure.vector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    @Test
    void shouldSplitLongTextAtSentenceBoundariesWithOverlap() {
        TextChunker chunker = new TextChunker(40, 80, 15);
        String text = "Alpha sentence explains uploads clearly. Beta sentence adds queue processing details. Gamma sentence covers chat retrieval steps.";

        List<String> chunks = chunker.chunk(text);

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0).endsWith("uploads clearly."));
        assertTrue(chunks.get(1).startsWith("uploads clearly."));
        assertTrue(chunks.get(2).contains("chat retrieval steps."));
    }

    @Test
    void shouldReturnSingleChunkWhenTextFitsWithinMaximumSize() {
        TextChunker chunker = new TextChunker(20, 80, 10);

        List<String> chunks = chunker.chunk("A short paragraph that already fits.");

        assertEquals(List.of("A short paragraph that already fits."), chunks);
    }
}

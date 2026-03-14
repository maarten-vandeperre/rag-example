package com.rag.app.infrastructure.vector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmbeddingGeneratorTest {

    @Test
    void shouldGenerateNormalizedEmbeddings() {
        EmbeddingGenerator generator = new EmbeddingGenerator(16);

        double[] embedding = generator.generateEmbedding("alpha beta beta");

        double squaredMagnitude = 0.0d;
        for (double value : embedding) {
            squaredMagnitude += value * value;
        }

        assertEquals(1.0d, Math.sqrt(squaredMagnitude), 0.0000001d);
    }

    @Test
    void shouldGenerateDifferentEmbeddingsForDifferentTexts() {
        EmbeddingGenerator generator = new EmbeddingGenerator(16);

        double[] alphaEmbedding = generator.generateEmbedding("alpha beta gamma");
        double[] financeEmbedding = generator.generateEmbedding("invoice revenue balance");

        double distance = 0.0d;
        for (int index = 0; index < alphaEmbedding.length; index++) {
            distance += Math.abs(alphaEmbedding[index] - financeEmbedding[index]);
        }

        assertNotEquals(0.0d, distance);
    }

    @Test
    void shouldRejectBlankText() {
        EmbeddingGenerator generator = new EmbeddingGenerator();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> generator.generateEmbedding("   "));

        assertEquals("text must not be null or empty", exception.getMessage());
    }
}

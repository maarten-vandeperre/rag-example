package com.rag.app.infrastructure.vector;

import javax.enterprise.context.ApplicationScoped;
import java.util.Locale;
import java.util.Objects;

@ApplicationScoped
public class EmbeddingGenerator {
    static final int DEFAULT_DIMENSIONS = 128;

    private final int dimensions;

    public EmbeddingGenerator() {
        this(DEFAULT_DIMENSIONS);
    }

    EmbeddingGenerator(int dimensions) {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        this.dimensions = dimensions;
    }

    public double[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be null or empty");
        }

        String[] tokens = text.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s]", " ")
            .trim()
            .split("\\s+");

        double[] vector = new double[dimensions];
        int tokenCount = 0;

        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }

            int index = Math.floorMod(Objects.hash(token), dimensions);
            vector[index] += 1.0d;
            tokenCount++;
        }

        if (tokenCount == 0) {
            throw new IllegalArgumentException("text must contain alphanumeric content");
        }

        normalize(vector);
        return vector;
    }

    private void normalize(double[] vector) {
        double magnitude = 0.0d;
        for (double value : vector) {
            magnitude += value * value;
        }

        magnitude = Math.sqrt(magnitude);
        if (magnitude == 0.0d) {
            throw new IllegalStateException("generated embedding must not be empty");
        }

        for (int index = 0; index < vector.length; index++) {
            vector[index] = vector[index] / magnitude;
        }
    }
}

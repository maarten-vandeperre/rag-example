package com.rag.app.infrastructure.vector;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TextChunker {
    static final int DEFAULT_MIN_CHUNK_SIZE = 500;
    static final int DEFAULT_MAX_CHUNK_SIZE = 1000;
    static final int DEFAULT_OVERLAP_SIZE = 100;

    private final int minChunkSize;
    private final int maxChunkSize;
    private final int overlapSize;

    public TextChunker() {
        this(DEFAULT_MIN_CHUNK_SIZE, DEFAULT_MAX_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    TextChunker(int minChunkSize, int maxChunkSize, int overlapSize) {
        if (minChunkSize <= 0) {
            throw new IllegalArgumentException("minChunkSize must be positive");
        }
        if (maxChunkSize < minChunkSize) {
            throw new IllegalArgumentException("maxChunkSize must be greater than or equal to minChunkSize");
        }
        if (overlapSize < 0 || overlapSize >= maxChunkSize) {
            throw new IllegalArgumentException("overlapSize must be between 0 and maxChunkSize - 1");
        }
        this.minChunkSize = minChunkSize;
        this.maxChunkSize = maxChunkSize;
        this.overlapSize = overlapSize;
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be null or empty");
        }

        String normalizedText = normalizeWhitespace(text);
        if (normalizedText.length() <= maxChunkSize) {
            return List.of(normalizedText);
        }

        List<String> chunks = new ArrayList<>();
        for (String sentence : splitSentences(normalizedText)) {
            appendSentence(chunks, sentence);
        }
        return List.copyOf(chunks);
    }

    private void appendSentence(List<String> chunks, String sentence) {
        if (sentence.length() > maxChunkSize) {
            appendLongSentence(chunks, sentence);
            return;
        }

        if (chunks.isEmpty()) {
            chunks.add(sentence);
            return;
        }

        String current = chunks.get(chunks.size() - 1);
        String candidate = current + " " + sentence;
        if (candidate.length() <= maxChunkSize) {
            chunks.set(chunks.size() - 1, candidate);
            return;
        }

        if (current.length() < minChunkSize && chunks.size() > 1) {
            String previous = chunks.get(chunks.size() - 2);
            String merged = previous + " " + current;
            if (merged.length() <= maxChunkSize) {
                chunks.set(chunks.size() - 2, merged);
                chunks.set(chunks.size() - 1, withOverlap(merged, sentence));
                return;
            }
        }

        chunks.add(withOverlap(current, sentence));
    }

    private void appendLongSentence(List<String> chunks, String sentence) {
        int start = 0;
        while (start < sentence.length()) {
            int end = Math.min(start + maxChunkSize, sentence.length());
            if (end < sentence.length()) {
                int boundary = sentence.lastIndexOf(' ', end);
                if (boundary > start + (minChunkSize / 2)) {
                    end = boundary;
                }
            }

            String piece = sentence.substring(start, end).trim();
            if (piece.isEmpty()) {
                break;
            }

            if (chunks.isEmpty()) {
                chunks.add(piece);
            } else {
                chunks.add(withOverlap(chunks.get(chunks.size() - 1), piece));
            }

            if (end == sentence.length()) {
                break;
            }
            start = Math.max(0, end - overlapSize);
            while (start < sentence.length() && sentence.charAt(start) == ' ') {
                start++;
            }
        }
    }

    private List<String> splitSentences(String text) {
        return List.of(text.split("(?<=[.!?])\\s+"));
    }

    private String withOverlap(String previousChunk, String nextSentence) {
        String overlap = overlap(previousChunk);
        if (overlap.isEmpty()) {
            return nextSentence;
        }
        return overlap + " " + nextSentence;
    }

    private String overlap(String text) {
        if (overlapSize == 0 || text.length() <= overlapSize) {
            return text;
        }

        int sentenceStart = lastSentenceStart(text);
        if (sentenceStart > 0) {
            return text.substring(sentenceStart).trim();
        }

        int start = text.length() - overlapSize;
        while (start > 0 && text.charAt(start - 1) != ' ') {
            start--;
        }

        return text.substring(start).trim();
    }

    private int lastSentenceStart(String text) {
        int period = text.lastIndexOf(". ");
        int question = text.lastIndexOf("? ");
        int exclamation = text.lastIndexOf("! ");
        int boundary = Math.max(period, Math.max(question, exclamation));
        if (boundary < 0) {
            return text.isBlank() ? 0 : 0;
        }
        return boundary + 2;
    }

    private String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}

package com.rag.app.infrastructure.document;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlainTextExtractorTest {

    @Test
    void shouldDecodeUtf8WithBom() {
        PlainTextExtractor extractor = new PlainTextExtractor();
        byte[] content = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] textBytes = "Hello plain text".getBytes(StandardCharsets.UTF_8);
        byte[] fileContent = new byte[content.length + textBytes.length];
        System.arraycopy(content, 0, fileContent, 0, content.length);
        System.arraycopy(textBytes, 0, fileContent, content.length, textBytes.length);

        assertEquals("Hello plain text", extractor.extract(fileContent));
    }

    @Test
    void shouldDecodeUtf16() {
        PlainTextExtractor extractor = new PlainTextExtractor();
        byte[] bom = new byte[]{(byte) 0xFF, (byte) 0xFE};
        byte[] text = "Hello unicode text".getBytes(StandardCharsets.UTF_16LE);
        byte[] fileContent = new byte[bom.length + text.length];
        System.arraycopy(bom, 0, fileContent, 0, bom.length);
        System.arraycopy(text, 0, fileContent, bom.length, text.length);

        assertEquals("Hello unicode text", extractor.extract(fileContent));
    }

    @Test
    void shouldRejectMalformedText() {
        PlainTextExtractor extractor = new PlainTextExtractor();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> extractor.extract(new byte[]{(byte) 0xC3, 0x28}));

        assertEquals("Failed to decode plain text document", exception.getMessage());
    }
}

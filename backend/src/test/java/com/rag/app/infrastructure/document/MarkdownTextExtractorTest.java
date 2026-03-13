package com.rag.app.infrastructure.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownTextExtractorTest {

    @Test
    void shouldStripMarkdownFormatting() {
        MarkdownTextExtractor extractor = new MarkdownTextExtractor();

        String result = extractor.extract("# Title\n\nThis is **bold** and [linked](https://example.com).\n- Item one\n`inline`");

        assertEquals("Title\n\nThis is bold and linked.\nItem one\ninline", result);
    }
}

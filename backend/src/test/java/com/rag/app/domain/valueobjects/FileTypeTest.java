package com.rag.app.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class FileTypeTest {

    @Test
    void shouldExposeSupportedFileTypes() {
        assertArrayEquals(
            new FileType[]{FileType.PDF, FileType.MARKDOWN, FileType.PLAIN_TEXT},
            FileType.values()
        );
    }
}

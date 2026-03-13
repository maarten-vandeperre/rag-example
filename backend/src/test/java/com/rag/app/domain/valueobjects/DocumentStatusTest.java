package com.rag.app.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class DocumentStatusTest {

    @Test
    void shouldExposeSupportedDocumentStatuses() {
        assertArrayEquals(
            new DocumentStatus[]{DocumentStatus.UPLOADED, DocumentStatus.PROCESSING, DocumentStatus.READY, DocumentStatus.FAILED},
            DocumentStatus.values()
        );
    }
}

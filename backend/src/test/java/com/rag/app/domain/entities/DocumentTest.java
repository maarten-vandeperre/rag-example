package com.rag.app.domain.entities;

import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentTest {

    @Test
    void shouldCreateDocumentWhenRequiredFieldsAreValid() {
        UUID documentId = UUID.randomUUID();
        Instant uploadedAt = Instant.parse("2026-03-13T10:15:30Z");
        DocumentMetadata metadata = new DocumentMetadata("knowledge-base.pdf", 4096L, FileType.PDF, "hash-123");

        Document document = new Document(documentId, metadata, "user-123", uploadedAt, DocumentStatus.UPLOADED);

        assertEquals(documentId, document.documentId());
        assertEquals("knowledge-base.pdf", document.fileName());
        assertEquals(4096L, document.fileSize());
        assertEquals(FileType.PDF, document.fileType());
        assertEquals("user-123", document.uploadedBy());
        assertEquals(uploadedAt, document.uploadedAt());
        assertEquals(DocumentStatus.UPLOADED, document.status());
        assertEquals("hash-123", document.contentHash());
    }

    @Test
    void shouldRejectDocumentWhenUploadedByIsBlank() {
        DocumentMetadata metadata = new DocumentMetadata("notes.md", 512L, FileType.MARKDOWN, "hash-456");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new Document(UUID.randomUUID(), metadata, " ", Instant.now(), DocumentStatus.READY));

        assertEquals("uploadedBy must not be null or empty", exception.getMessage());
    }

    @Test
    void shouldRejectDocumentWhenUploadedAtIsNull() {
        DocumentMetadata metadata = new DocumentMetadata("notes.txt", 256L, FileType.PLAIN_TEXT, "hash-789");

        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> new Document(UUID.randomUUID(), metadata, "user-456", null, DocumentStatus.PROCESSING));

        assertEquals("uploadedAt must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectMetadataWhenFileSizeExceedsLimit() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new DocumentMetadata("large.pdf", DocumentMetadata.MAX_FILE_SIZE_BYTES + 1L, FileType.PDF, "hash-999"));

        assertEquals("fileSize must be positive and less than or equal to 41943040", exception.getMessage());
    }

    @Test
    void shouldRejectMetadataWhenFileNameIsNullOrBlank() {
        IllegalArgumentException nullFileNameException = assertThrows(IllegalArgumentException.class,
            () -> new DocumentMetadata(null, 128L, FileType.PLAIN_TEXT, "hash-1000"));
        IllegalArgumentException blankFileNameException = assertThrows(IllegalArgumentException.class,
            () -> new DocumentMetadata("   ", 128L, FileType.PLAIN_TEXT, "hash-1001"));

        assertEquals("fileName must not be null or empty", nullFileNameException.getMessage());
        assertEquals("fileName must not be null or empty", blankFileNameException.getMessage());
    }
}

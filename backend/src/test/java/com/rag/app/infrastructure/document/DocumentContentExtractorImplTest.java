package com.rag.app.infrastructure.document;

import com.rag.app.domain.valueobjects.FileType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentContentExtractorImplTest {

    @Test
    void shouldExtractPdfMarkdownAndPlainText() throws IOException {
        DocumentContentExtractorImpl extractor = new DocumentContentExtractorImpl(new PdfTextExtractor(), new MarkdownTextExtractor(), new PlainTextExtractor());

        assertEquals("Long readable pdf text", extractor.extractText(pdfBytes("Long readable pdf text"), FileType.PDF));
        assertEquals("Heading\n\nMarkdown content here", extractor.extractText("# Heading\n\nMarkdown content here".getBytes(StandardCharsets.UTF_8), FileType.MARKDOWN));
        assertEquals("Long plain text", extractor.extractText("Long plain text".getBytes(StandardCharsets.UTF_8), FileType.PLAIN_TEXT));
    }

    @Test
    void shouldRejectShortOrEmptyContent() {
        DocumentContentExtractorImpl extractor = new DocumentContentExtractorImpl(new PdfTextExtractor(), new MarkdownTextExtractor(), new PlainTextExtractor());

        IllegalArgumentException emptyException = assertThrows(IllegalArgumentException.class,
            () -> extractor.extractText("   ".getBytes(StandardCharsets.UTF_8), FileType.PLAIN_TEXT));
        IllegalArgumentException shortException = assertThrows(IllegalArgumentException.class,
            () -> extractor.extractText("short".getBytes(StandardCharsets.UTF_8), FileType.PLAIN_TEXT));

        assertEquals("Document content is empty or unreadable", emptyException.getMessage());
        assertEquals("Document content must contain at least 10 readable characters", shortException.getMessage());
    }

    private static byte[] pdfBytes(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}

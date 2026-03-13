package com.rag.app.infrastructure.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfTextExtractorTest {

    @Test
    void shouldExtractTextFromPdf() throws IOException {
        PdfTextExtractor extractor = new PdfTextExtractor();

        assertEquals("Readable PDF content", extractor.extract(pdfBytes("Readable PDF content")));
    }

    @Test
    void shouldRejectUnreadablePdf() {
        PdfTextExtractor extractor = new PdfTextExtractor();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> extractor.extract(new byte[]{1, 2, 3}));

        assertEquals("Failed to extract text from PDF document", exception.getMessage());
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

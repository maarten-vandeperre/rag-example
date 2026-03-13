package com.rag.app.infrastructure.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

public final class PdfTextExtractor {

    public String extract(byte[] fileContent) {
        try (PDDocument document = PDDocument.load(fileContent)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("Password-protected PDF files are not supported");
            }

            String extractedText = new PDFTextStripper().getText(document);
            if (extractedText == null || extractedText.isBlank()) {
                throw new IllegalArgumentException("PDF does not contain readable text");
            }

            return extractedText.strip();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to extract text from PDF document", exception);
        }
    }
}

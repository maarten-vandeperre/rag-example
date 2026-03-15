package com.rag.app.infrastructure.document;

import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.usecases.interfaces.DocumentContentExtractor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@ApplicationScoped
public class DocumentContentExtractorImpl implements DocumentContentExtractor {
    private static final int MIN_CONTENT_LENGTH = 10;

    private final PdfTextExtractor pdfTextExtractor;
    private final MarkdownTextExtractor markdownTextExtractor;
    private final PlainTextExtractor plainTextExtractor;

    @Inject
    public DocumentContentExtractorImpl() {
        this(new PdfTextExtractor(), new MarkdownTextExtractor(), new PlainTextExtractor());
    }

    DocumentContentExtractorImpl(PdfTextExtractor pdfTextExtractor,
                                 MarkdownTextExtractor markdownTextExtractor,
                                 PlainTextExtractor plainTextExtractor) {
        this.pdfTextExtractor = Objects.requireNonNull(pdfTextExtractor, "pdfTextExtractor must not be null");
        this.markdownTextExtractor = Objects.requireNonNull(markdownTextExtractor, "markdownTextExtractor must not be null");
        this.plainTextExtractor = Objects.requireNonNull(plainTextExtractor, "plainTextExtractor must not be null");
    }

    @Override
    public String extractText(byte[] fileContent, FileType fileType) {
        if (fileContent == null || fileContent.length == 0) {
            throw new IllegalArgumentException("Document content must not be null or empty");
        }
        Objects.requireNonNull(fileType, "fileType must not be null");

        String extractedText = switch (fileType) {
            case PDF -> pdfTextExtractor.extract(fileContent);
            case MARKDOWN -> markdownTextExtractor.extract(new String(fileContent, StandardCharsets.UTF_8));
            case PLAIN_TEXT -> plainTextExtractor.extract(fileContent);
        };

        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException("Document content is empty or unreadable");
        }
        if (extractedText.strip().length() < MIN_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Document content must contain at least 10 readable characters");
        }

        return extractedText.strip();
    }
}

package com.rag.app.document.infrastructure.processing;

import com.rag.app.document.domain.valueobjects.FileType;
import com.rag.app.document.interfaces.DocumentContentExtractor;

import java.nio.charset.StandardCharsets;

public final class DocumentContentExtractorImpl implements DocumentContentExtractor {
    @Override
    public String extractText(byte[] fileContent, FileType fileType) {
        String text = new String(fileContent, StandardCharsets.UTF_8).trim();
        if (fileType == FileType.PDF && text.isEmpty()) {
            return "PDF content extraction requires binary parser integration";
        }
        return text;
    }
}

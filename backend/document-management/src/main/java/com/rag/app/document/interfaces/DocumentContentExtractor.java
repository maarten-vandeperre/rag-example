package com.rag.app.document.interfaces;

import com.rag.app.document.domain.valueobjects.FileType;

public interface DocumentContentExtractor {
    String extractText(byte[] fileContent, FileType fileType);
}

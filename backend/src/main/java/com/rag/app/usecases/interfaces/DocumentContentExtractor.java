package com.rag.app.usecases.interfaces;

import com.rag.app.domain.valueobjects.FileType;

public interface DocumentContentExtractor {
    String extractText(byte[] fileContent, FileType fileType);
}

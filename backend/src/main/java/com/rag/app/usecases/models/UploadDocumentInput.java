package com.rag.app.usecases.models;

import com.rag.app.domain.valueobjects.FileType;

import java.util.Arrays;
import java.util.UUID;

public record UploadDocumentInput(String fileName,
                                  long fileSize,
                                  FileType fileType,
                                  byte[] fileContent,
                                  UUID uploadedBy) {
    public UploadDocumentInput {
        fileContent = fileContent == null ? null : Arrays.copyOf(fileContent, fileContent.length);
    }

    @Override
    public byte[] fileContent() {
        return fileContent == null ? null : Arrays.copyOf(fileContent, fileContent.length);
    }
}

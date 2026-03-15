package com.rag.app.document.usecases.models;

import com.rag.app.document.domain.valueobjects.FileType;

public record UploadDocumentInput(String fileName, long fileSize, FileType fileType, byte[] fileContent, String uploadedBy) {
}

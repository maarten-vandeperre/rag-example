package com.rag.app.document.usecases.models;

public record GetUserDocumentsInput(String userId, boolean includeAllDocuments) {
}

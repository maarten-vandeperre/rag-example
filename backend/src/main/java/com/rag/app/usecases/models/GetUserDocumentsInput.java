package com.rag.app.usecases.models;

import java.util.UUID;

public record GetUserDocumentsInput(UUID userId, boolean includeAllDocuments) {
}

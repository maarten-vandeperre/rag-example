package com.rag.app.document.usecases.models;

import java.util.List;

public record GetUserDocumentsOutput(List<DocumentSummary> documents, int totalCount) {
}

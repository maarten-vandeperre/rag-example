package com.rag.app.usecases.models;

import java.util.List;

public record GetUserDocumentsOutput(List<DocumentSummary> documents, int totalCount) {
}

package com.rag.app.api.dto;

import java.util.List;

public record DocumentListResponse(List<DocumentSummaryDto> documents, int totalCount) {
}

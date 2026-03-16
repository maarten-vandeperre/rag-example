package com.rag.app.integration.api.dto.knowledge;

import java.util.List;

public record KnowledgeSearchResultDto(String query,
                                       List<KnowledgeNodeDto> nodes,
                                       List<KnowledgeRelationshipDto> relationships,
                                       List<KnowledgeGraphSummaryDto> graphs,
                                       int totalResults) {
}

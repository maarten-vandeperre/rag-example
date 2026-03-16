package com.rag.app.integration.api.dto.knowledge;

import java.util.List;

public record KnowledgeSubgraphDto(String centerNodeId,
                                   int depth,
                                   List<KnowledgeNodeDto> nodes,
                                   List<KnowledgeRelationshipDto> relationships,
                                   GraphVisualizationDto visualization) {
}

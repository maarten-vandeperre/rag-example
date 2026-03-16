package com.rag.app.integration.api.dto.knowledge;

import java.util.List;

public record KnowledgeNodeDetailDto(KnowledgeNodeDto node,
                                     List<KnowledgeNodeDto> connectedNodes,
                                     List<KnowledgeRelationshipDto> relationships,
                                     int connectionCount,
                                     List<String> relationshipTypes) {
}

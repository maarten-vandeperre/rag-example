package com.rag.app.api.dto;

import java.util.List;

public record AnswerSourceDetailsResponse(String answerId,
                                          List<AnswerSourceDetailDto> sources,
                                          int totalSources,
                                          int availableSources) {

    public AnswerSourceDetailsResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}

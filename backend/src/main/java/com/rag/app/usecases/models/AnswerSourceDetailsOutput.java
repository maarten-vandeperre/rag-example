package com.rag.app.usecases.models;

import java.util.List;
import java.util.UUID;

public record AnswerSourceDetailsOutput(UUID answerId,
                                        List<AnswerSourceDetail> sources,
                                        int totalSources,
                                        int availableSources) {

    public AnswerSourceDetailsOutput {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}

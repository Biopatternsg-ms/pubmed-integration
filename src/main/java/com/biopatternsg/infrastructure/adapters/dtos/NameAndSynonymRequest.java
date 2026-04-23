package com.biopatternsg.infrastructure.adapters.dtos;

public record NameAndSynonymRequest(
        String pipelineId,
        int level
) {
}

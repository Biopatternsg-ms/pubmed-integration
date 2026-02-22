package com.biopatternsg.infrastructure.adapters.dtos;

public record BuildPairsRequest(
        String pipelineId,
        boolean useShortName,
        int levels
) {
}

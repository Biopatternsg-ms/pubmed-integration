package com.biopatternsg.infrastructure.adapters.dtos;

public record BuildPairsRequest(
        String pipelineId,
        boolean useOnlyPrincipalName,
        int levels
) {
}

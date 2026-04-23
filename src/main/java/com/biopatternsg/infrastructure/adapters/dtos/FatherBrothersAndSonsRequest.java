package com.biopatternsg.infrastructure.adapters.dtos;

public record FatherBrothersAndSonsRequest(
        String pipelineId,
        String biologicalObjectId
) {
}

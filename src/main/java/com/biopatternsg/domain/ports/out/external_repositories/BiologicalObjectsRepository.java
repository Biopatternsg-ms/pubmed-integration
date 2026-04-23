package com.biopatternsg.domain.ports.out.external_repositories;

import com.biopatternsg.domain.model.BiologicalObject;

import java.util.List;

public interface BiologicalObjectsRepository {
    List<BiologicalObject> biologicalObjectsByPipelineAndLevel(String pipelineId, int level);
    List<BiologicalObject> biologicalObjectFatherBrothersAndSons(String pipelineId, String biologicalObjectId);
}

package com.biopatternsg.application.services;

import com.biopatternsg.domain.model.BiologicalObject;

import java.util.List;

public interface BiologicalObjectsService {
    List<BiologicalObject> getBiologicalObjectsByLevel(String pipelineId, int level);
    List<BiologicalObject> getBiologicalObjectFatherBrothersAndSons(String pipelineId, String biologicalObjectId);
}

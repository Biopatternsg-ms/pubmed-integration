package com.biopatternsg.application.services;

import com.biopatternsg.domain.model.BiologicalObject;

import java.util.List;

public interface BiologicalObjectsService {
    List<BiologicalObject> getBiologicalObjects(String pipelineId, int level);
}

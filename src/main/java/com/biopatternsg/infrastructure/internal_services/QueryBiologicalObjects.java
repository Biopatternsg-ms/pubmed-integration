package com.biopatternsg.infrastructure.internal_services;

import com.biopatternsg.domain.model.BiologicalObject;

import java.util.List;

public interface QueryBiologicalObjects {
    List<BiologicalObject> biologicalObjects(String pipelineId, int level);
}

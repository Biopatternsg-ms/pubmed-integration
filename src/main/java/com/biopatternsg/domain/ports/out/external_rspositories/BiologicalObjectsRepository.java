package com.biopatternsg.domain.ports.out.external_rspositories;

import com.biopatternsg.domain.model.BiologicalObject;

import java.util.List;

public interface BiologicalObjectsRepository {
    List<BiologicalObject> biologicalObjects(String pipelineId, int level);
}

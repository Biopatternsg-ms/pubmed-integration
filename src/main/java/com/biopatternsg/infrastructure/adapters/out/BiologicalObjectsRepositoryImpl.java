package com.biopatternsg.infrastructure.adapters.out;

import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.domain.ports.out.external_repositories.BiologicalObjectsRepository;
import com.biopatternsg.infrastructure.internal_services.QueryBiologicalObjects;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class BiologicalObjectsRepositoryImpl implements BiologicalObjectsRepository {

    private final QueryBiologicalObjects queryBiologicalObjects;

    @Override
    public List<BiologicalObject> biologicalObjectsByPipelineAndLevel(String pipelineId, int level) {
        return queryBiologicalObjects.biologicalObjectsByPipelineAndLevel(pipelineId, level);
    }

    @Override
    public List<BiologicalObject> biologicalObjectFatherBrothersAndSons(String pipelineId, String biologicalObjectId) {
        return queryBiologicalObjects.biologicalObjectFatherBrothersAndSons(pipelineId, biologicalObjectId);
    }
}

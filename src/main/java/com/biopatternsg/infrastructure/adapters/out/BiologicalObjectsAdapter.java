package com.biopatternsg.infrastructure.adapters.out;

import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.domain.ports.out.external_rspositories.BiologicalObjectsRepository;
import com.biopatternsg.infrastructure.internal_services.QueryBiologicalObjects;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class BiologicalObjectsAdapter implements BiologicalObjectsRepository {

    private final QueryBiologicalObjects queryBiologicalObjects;

    @Override
    public List<BiologicalObject> biologicalObjects(String pipelineId, int level) {
        return List.of();
    }
}

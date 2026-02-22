package com.biopatternsg.application.services.impl;

import com.biopatternsg.application.services.BiologicalObjectsService;
import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.domain.ports.out.external_rspositories.BiologicalObjectsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class BiologicalObjectsServiceImpl implements BiologicalObjectsService {

    private final BiologicalObjectsRepository biologicalObjectsRepository;

    @Override
    public List<BiologicalObject> getBiologicalObjects(String pipelineId, int level) {
        return biologicalObjectsRepository.biologicalObjects(pipelineId, level);
    }
}

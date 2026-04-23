package com.biopatternsg.application.services.impl;

import com.biopatternsg.application.services.BiologicalObjectsService;
import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.domain.ports.out.external_repositories.BiologicalObjectsRepository;
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
    public List<BiologicalObject> getBiologicalObjectsByLevel(String pipelineId, int level) {
        return biologicalObjectsRepository.biologicalObjectsByPipelineAndLevel(pipelineId, level);
    }

    @Override
    public List<BiologicalObject> getBiologicalObjectFatherBrothersAndSons(String pipelineId, String biologicalObjectId) {
        return biologicalObjectsRepository.biologicalObjectFatherBrothersAndSons(pipelineId, biologicalObjectId);
    }
}

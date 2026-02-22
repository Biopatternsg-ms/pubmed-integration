package com.biopatternsg.application.usecase;

import com.biopatternsg.application.services.BiologicalObjectsService;
import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.domain.ports.in.BuildPubmedPairs;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class BuildPubmedPairsUseCase implements BuildPubmedPairs {

    private final BiologicalObjectsService biologicalObjectsService;
    private static final int LEVEL_FIRST = 1;

    @Override
    public void execute(String pipelineId, boolean useShortName, int levels) {
        //Busqueda nivel 1
        List<BiologicalObject> biologicalObjects = biologicalObjectsService.getBiologicalObjects(pipelineId, LEVEL_FIRST);

    }
}

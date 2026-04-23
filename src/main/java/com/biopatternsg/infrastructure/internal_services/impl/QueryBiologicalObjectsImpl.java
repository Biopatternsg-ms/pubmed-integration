package com.biopatternsg.infrastructure.internal_services.impl;

import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.infrastructure.adapters.dtos.FatherBrothersAndSonsRequest;
import com.biopatternsg.infrastructure.adapters.dtos.NameAndSynonymRequest;
import com.biopatternsg.infrastructure.clients.internal_clients.BiologicalObjectsHttpClient;
import com.biopatternsg.infrastructure.internal_services.QueryBiologicalObjects;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@ApplicationScoped
public class QueryBiologicalObjectsImpl implements QueryBiologicalObjects {

    private final BiologicalObjectsHttpClient biologicalObjectsHttpClient;

    public QueryBiologicalObjectsImpl(@RestClient BiologicalObjectsHttpClient biologicalObjectsHttpClient) {
        this.biologicalObjectsHttpClient = biologicalObjectsHttpClient;
    }

    @Override
    public List<BiologicalObject> biologicalObjectsByPipelineAndLevel(String pipelineId, int level) {
        return biologicalObjectsHttpClient.biologicalObjectsByPipelineAndLevel(new NameAndSynonymRequest(pipelineId, level));
    }

    @Override
    public List<BiologicalObject> biologicalObjectFatherBrothersAndSons(String pipelineId, String biologicalObjectId) {
        return biologicalObjectsHttpClient.biologicalObjectFatherBrothersAndSons(new FatherBrothersAndSonsRequest(pipelineId, biologicalObjectId));
    }
}

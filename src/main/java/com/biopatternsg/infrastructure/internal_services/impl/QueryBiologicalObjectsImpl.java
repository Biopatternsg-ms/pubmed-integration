package com.biopatternsg.infrastructure.internal_services.impl;

import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.infrastructure.clients.internal_clients.BiologicalObjectsHttpClient;
import com.biopatternsg.infrastructure.internal_services.QueryBiologicalObjects;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

public class QueryBiologicalObjectsImpl implements QueryBiologicalObjects {

    private final BiologicalObjectsHttpClient biologicalObjectsHttpClient;

    public QueryBiologicalObjectsImpl(@RestClient BiologicalObjectsHttpClient biologicalObjectsHttpClient) {
        this.biologicalObjectsHttpClient = biologicalObjectsHttpClient;
    }

    @Override
    public List<BiologicalObject> biologicalObjects(String pipelineId, int level) {
        return List.of();
    }
}

package com.biopatternsg.infrastructure.clients.internal_clients;

import com.biopatternsg.domain.model.BiologicalObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "biological-objects-api")
public interface BiologicalObjectsHttpClient {

    @GET
    @Path("biological-object/name-and-synonyms")
    List<BiologicalObject> biologicalObjects(String pipelineId, int level);

}

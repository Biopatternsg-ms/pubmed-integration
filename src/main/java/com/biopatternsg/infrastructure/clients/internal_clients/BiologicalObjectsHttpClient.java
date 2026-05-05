package com.biopatternsg.infrastructure.clients.internal_clients;

import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.infrastructure.adapters.dtos.FatherBrothersAndSonsRequest;
import com.biopatternsg.infrastructure.adapters.dtos.NameAndSynonymRequest;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "biological-objects-api")
public interface BiologicalObjectsHttpClient {

    @POST
    @Path("biological-object/name-and-synonyms")
    List<BiologicalObject> biologicalObjectsByPipelineAndLevel(NameAndSynonymRequest nameAndSynonymRequest);

    @POST
    @Path("biological-object/father-brothers-and-sons")
    List<BiologicalObject> biologicalObjectFatherBrothersAndSons(FatherBrothersAndSonsRequest fatherBrothersAndSonsRequest);

}

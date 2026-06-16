/*
 * Copyright © 2026 biopatternsg (biopatternsg@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.biopatternsg.infrastructure.clients.internal;

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

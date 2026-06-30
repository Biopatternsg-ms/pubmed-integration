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
package com.biopatternsg.infrastructure.internal_services.impl;

import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.infrastructure.adapters.dtos.FatherBrothersAndSonsRequest;
import com.biopatternsg.infrastructure.adapters.dtos.NameAndSynonymRequest;
import com.biopatternsg.infrastructure.clients.internal.BiologicalObjectsHttpClient;
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

    @Override
    public List<BiologicalObject> expertObjectsByPipelineAndLevel(String pipelineId, int level) {
        return biologicalObjectsHttpClient.expertObjectsByPipelineAndLevel(new NameAndSynonymRequest(pipelineId, level));
    }
}

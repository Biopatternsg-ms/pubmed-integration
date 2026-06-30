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
package com.biopatternsg.infrastructure.adapters.in;

import com.biopatternsg.domain.ports.in.BuildPubmedPairs;
import com.biopatternsg.domain.ports.in.SearchPubmedByPairs;
import com.biopatternsg.domain.ports.in.SearchPubtatorByPmids;
import com.biopatternsg.domain.ports.in.GenerateKbForPipeline;
import com.biopatternsg.domain.ports.in.GenerateAlignedObjects;
import com.biopatternsg.infrastructure.adapters.dtos.BuildPairsRequest;
import com.biopatternsg.infrastructure.adapters.dtos.SearchPairsByPipelineRequest;
import com.biopatternsg.infrastructure.adapters.dtos.SearchPubtatorByPipelineRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.biopatternsg.infrastructure.session.SessionUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
@Path("/pubmed")
public class PubmedController {

    private final BuildPubmedPairs buildPubmedPairs;
    private final SearchPubmedByPairs searchPubmedByPairs;
    private final SearchPubtatorByPmids searchPubtatorByPmids;
    private final GenerateKbForPipeline generateKbForPipeline;
    private final GenerateAlignedObjects generateAlignedObjects;
    private final Executor executor;
    private final SessionUtils sessionUtils;

    @POST
    @Path("/build-pairs")
    @ActivateRequestContext
    public Response buildTreeMesh(@Valid BuildPairsRequest buildPairsRequest) {

        String userId = sessionUtils.getUserId();
        CompletableFuture.runAsync(() -> {
            try {
                buildPubmedPairs.execute(buildPairsRequest.pipelineId(), buildPairsRequest.useOnlyPrincipalName(), buildPairsRequest.levels(), userId);
            } catch (Exception e) {
                log.error("Error building pubmed pairs", e);
            }
        }, executor);

        return Response.accepted()
                .entity("{\"message\": \"Buildind pubmed pairs\"}")
                .build();
    }

    @POST
    @Path("/search-pubmed-ids-by-pairs")
    @ActivateRequestContext
    public Response searchPairs(
            @Valid SearchPairsByPipelineRequest request
    ) {

        String userId = sessionUtils.getUserId();
        CompletableFuture.runAsync(() -> {
            try {
                searchPubmedByPairs.execute(request.pipelineId(), request.retmax(), userId);
            } catch (Exception e) {
                log.error("Error searching pubmed pairs", e);
            }
        }, executor);

        return Response.accepted()
                .entity("{\"message\": \"Searching pubmed IDS by pairs\"}")
                .build();
    }

    @POST
    @Path("/search-pubtator-by-pmids")
    @ActivateRequestContext
    public Response searchPubtatorByPmids(
            @Valid SearchPubtatorByPipelineRequest request
    ) {

        String userId = sessionUtils.getUserId();
        CompletableFuture.runAsync(() -> {
            try {
                searchPubtatorByPmids.execute(request.pipelineId(), userId);
            } catch (Exception e) {
                log.error("Error searching PubTator by pmids", e);
            }
        }, executor);

        return Response.accepted()
                .entity("{\"message\": \"Searching PubTator by pmids\"}")
                .build();
    }

    @POST
    @Path("/generate-kb")
    @ActivateRequestContext
    public Response generateKb(
            @Valid SearchPubtatorByPipelineRequest request
    ) {

        String userId = sessionUtils.getUserId();
        CompletableFuture.runAsync(() -> {
            try {
                generateKbForPipeline.execute(request.pipelineId(), userId);
            } catch (Exception e) {
                log.error("Error generating KB for pipeline", e);
            }
        }, executor);

        return Response.accepted()
                .entity("{\"message\": \"Knowledge base generation pipeline started\"}")
                .build();
    }

    @POST
    @Path("/generate-aligned-objects")
    @ActivateRequestContext
    public Response generateAlignedObjects(
            @Valid SearchPubtatorByPipelineRequest request
    ) {

        CompletableFuture.runAsync(() -> {
            try {
                generateAlignedObjects.execute(request.pipelineId());
            } catch (Exception e) {
                log.error("Error generating aligned objects", e);
            }
        }, executor);

        return Response.accepted()
                .entity("{\"message\": \"Aligned objects generation started\"}")
                .build();
    }

}

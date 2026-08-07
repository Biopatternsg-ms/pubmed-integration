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
package com.biopatternsg.infrastructure.adapters.in.restcontrollers;

import com.biopatternsg.domain.ports.in.BuildPubmedPairs;
import com.biopatternsg.domain.ports.in.SearchPubmedByPairs;
import com.biopatternsg.domain.ports.in.SearchPubtatorByPmids;
import com.biopatternsg.domain.ports.in.GenerateKbForPipeline;
import com.biopatternsg.domain.ports.in.GenerateAlignedObjects;
import com.biopatternsg.domain.ports.in.GetPaginatedSynonyms;
import com.biopatternsg.domain.ports.in.GetSynonymsByName;
import com.biopatternsg.domain.ports.in.GetAlignedResults;
import com.biopatternsg.infrastructure.adapters.dtos.BuildPairsRequest;
import com.biopatternsg.infrastructure.adapters.dtos.GenerateAlignedObjectsRequest;
import com.biopatternsg.infrastructure.adapters.dtos.SearchPairsByPipelineRequest;
import com.biopatternsg.infrastructure.adapters.dtos.SearchPubtatorByPipelineRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
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
    private final GetPaginatedSynonyms getPaginatedSynonyms;
    private final GetSynonymsByName getSynonymsByName;
    private final GetAlignedResults getAlignedResults;
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
            @Valid GenerateAlignedObjectsRequest request
    ) {

        String userId = sessionUtils.getUserId();
        CompletableFuture.runAsync(() -> {
            try {
                generateAlignedObjects.execute(request.pipelineId(), request.expertObjects(), userId);
            } catch (Exception e) {
                log.error("Error generating aligned objects", e);
            }
        }, executor);

        return Response.accepted()
                .entity("{\"message\": \"Aligned objects generation started\"}")
                .build();
    }

    @GET
    @Path("/synonyms/{pipelineId}")
    public Response getSynonyms(
            @PathParam("pipelineId") String pipelineId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size
    ) {
        log.info("Request to get synonyms for pipelineId=[{}], page=[{}], size=[{}]", pipelineId, page, size);
        var paginatedResult = getPaginatedSynonyms.execute(pipelineId, page, size);
        return Response.ok(paginatedResult).build();
    }

    @GET
    @Path("/synonyms/{pipelineId}/by-name/{name}")
    public Response getSynonymsByName(
            @PathParam("pipelineId") String pipelineId,
            @PathParam("name") String name
    ) {
        log.info("Request to get synonyms for pipelineId=[{}], name=[{}]", pipelineId, name);
        if (name == null || name.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Path parameter 'name' is required\"}")
                    .build();
        }
        return getSynonymsByName.execute(pipelineId, name)
                .map(result -> Response.ok(result).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Synonyms not found for pipelineId: " + pipelineId + " and name: " + name + "\"}")
                        .build());
    }

    @GET
    @Path("/aligned-results/{pipelineId}")
    public Response getAlignedResults(@PathParam("pipelineId") String pipelineId) {
        log.info("Request to get aligned results for pipelineId=[{}]", pipelineId);
        return getAlignedResults.execute(pipelineId)
                .map(result -> Response.ok(result).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Aligned results not found for pipelineId: " + pipelineId + "\"}")
                        .build());
    }

}

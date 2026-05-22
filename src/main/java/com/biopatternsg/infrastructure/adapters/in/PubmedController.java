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
import com.biopatternsg.infrastructure.adapters.dtos.BuildPairsRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
@Path("/pubmed")
public class PubmedController {

    private final BuildPubmedPairs buildPubmedPairs;
    private final Executor executor;

    @POST
    @Path("/build-pairs")
    public Response buildTreeMesh(@Valid BuildPairsRequest buildPairsRequest) {

        CompletableFuture.runAsync(() -> {
            try {
                buildPubmedPairs.execute(buildPairsRequest.pipelineId(), buildPairsRequest.useOnlyPrincipalName(), buildPairsRequest.levels());
            } catch (Exception e) {
                log.error("Error building pubmed pairs", e);
            }
        }, executor);

        return Response.accepted()
                .entity("{\"message\": \"Buildind pubmed pairs\"}")
                .build();
    }

}

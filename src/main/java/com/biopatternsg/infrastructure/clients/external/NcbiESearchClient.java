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
package com.biopatternsg.infrastructure.clients.external;

import com.biopatternsg.infrastructure.clients.dtos.NcbiESearchResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ncbi-esearch-api")
public interface NcbiESearchClient {

    @POST
    @Path("/esearch.fcgi")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    NcbiESearchResponse search(
            @FormParam("db") String db,
            @FormParam("term") String term,
            @FormParam("retmax") int retmax,
            @FormParam("retmode") String retmode,
            @FormParam("tool") String tool,
            @FormParam("email") String email,
            @FormParam("sort") String sort,
            @FormParam("api_key") String apiKey
    );
}

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
package com.biopatternsg.infrastructure.adapters.out;

import com.biopatternsg.domain.model.NcbiSearchResult;
import com.biopatternsg.domain.ports.out.external_repositories.NcbiESearchPort;
import com.biopatternsg.infrastructure.clients.NcbiESearchClient;
import com.biopatternsg.infrastructure.clients.dtos.NcbiESearchResponse;
import com.biopatternsg.infrastructure.clients.dtos.NcbiESearchResultDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class NcbiESearchAdapter implements NcbiESearchPort {

    @Inject
    @RestClient
    NcbiESearchClient ncbiESearchClient;

    @Inject
    @ConfigProperty(name = "ncbi.esearch.tool", defaultValue = "pubmed-integration")
    String tool;

    @Inject
    @ConfigProperty(name = "ncbi.esearch.email", defaultValue = "biopatternsg@gmail.com")
    String email;

    @Override
    public NcbiSearchResult search(String term, int retmax) {
        var response = ncbiESearchClient.search("pubmed", term, retmax, "json", tool, email);

        NcbiESearchResultDto result = Optional.ofNullable(response)
                .map(NcbiESearchResponse::getEsearchresult)
                .orElse(null);

        if (result == null) {
            log.warn("Empty response from NCBI for term=[{}]", term);
            return new NcbiSearchResult(term, 0, Collections.emptyList());
        }

        int count = parseCount(result.getCount());
        List<String> ids = Optional.ofNullable(result.getIdList()).orElse(Collections.emptyList());

        return new NcbiSearchResult(term, count, ids);
    }

    private int parseCount(String count) {
        try {
            return count != null ? Integer.parseInt(count.trim()) : 0;
        } catch (NumberFormatException e) {
            log.warn("Could not parse NCBI count value: [{}]", count);
            return 0;
        }
    }
}

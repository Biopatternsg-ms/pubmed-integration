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
package com.biopatternsg.infrastructure.external_services.impl;

import com.biopatternsg.domain.model.NcbiSearchResult;
import com.biopatternsg.infrastructure.clients.external.NcbiESearchClient;
import com.biopatternsg.infrastructure.clients.dtos.NcbiESearchResponse;
import com.biopatternsg.infrastructure.clients.dtos.NcbiESearchResultDto;
import com.biopatternsg.infrastructure.external_services.QueryNcbiESearch;
import com.biopatternsg.infrastructure.util.SimpleRateLimiter;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class QueryNcbiESearchImpl implements QueryNcbiESearch {

    private final NcbiESearchClient ncbiESearchClient;
    private final String tool;
    private final String email;
    private final String apiKey;
    private final SimpleRateLimiter rateLimiter;

    public QueryNcbiESearchImpl(
            @RestClient NcbiESearchClient ncbiESearchClient,
            @ConfigProperty(name = "ncbi.esearch.tool") String tool,
            @ConfigProperty(name = "ncbi.esearch.email") String email,
            @ConfigProperty(name = "ncbi.esearch.api-key") String apiKey) {
        this.ncbiESearchClient = ncbiESearchClient;
        this.tool = tool;
        this.email = email;
        this.apiKey = apiKey;
        boolean hasValidKey = apiKey != null && !apiKey.trim().isEmpty()
                && !apiKey.equalsIgnoreCase("none")
                && !apiKey.equalsIgnoreCase("null")
                && !apiKey.contains("${");
        double permitsPerSecond = hasValidKey ? 8.0 : 2.0;
        this.rateLimiter = new SimpleRateLimiter(permitsPerSecond);
    }

    @Override
    public NcbiSearchResult search(String term, int retmax) {
        rateLimiter.acquire();
        String apiKeyParam = (apiKey == null || apiKey.trim().isEmpty()) ? null : apiKey.trim();
        NcbiESearchResponse response = ncbiESearchClient.search("pubmed", term, retmax, "json", tool, email, "relevance", apiKeyParam);

        NcbiESearchResultDto result = Optional.ofNullable(response)
                .map(NcbiESearchResponse::getESearchResult)
                .orElse(null);

        if (result == null) {
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
            return 0;
        }
    }
}

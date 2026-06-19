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

import com.biopatternsg.infrastructure.clients.external.PubtatorClient;
import com.biopatternsg.infrastructure.external_services.QueryPubtator;
import com.biopatternsg.infrastructure.util.SimpleRateLimiter;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@Slf4j
@ApplicationScoped
public class QueryPubtatorImpl implements QueryPubtator {

    private final PubtatorClient pubtatorClient;
    private final SimpleRateLimiter rateLimiter;

    public QueryPubtatorImpl(
            @RestClient PubtatorClient pubtatorClient,
            @ConfigProperty(name = "pubtator.rate-limit") double rateLimit) {
        this.pubtatorClient = pubtatorClient;
        this.rateLimiter = new SimpleRateLimiter(rateLimit);
    }

    @Override
    public String search(List<String> pmids) {
        if (pmids == null || pmids.isEmpty()) {
            return "";
        }
        
        List<String> cleanPmids = pmids.stream()
                .filter(pmid -> pmid != null && pmid.trim().matches("\\d+"))
                .toList();
                
        if (cleanPmids.isEmpty()) {
            log.warn("No valid numeric PMIDs in list to query PubTator.");
            return "";
        }

        rateLimiter.acquire();
        String pmidsParam = String.join(",", cleanPmids);
        log.debug("Querying PubTator for PMIDs: {}", pmidsParam);
        return pubtatorClient.exportBiocJson(pmidsParam);
    }
}

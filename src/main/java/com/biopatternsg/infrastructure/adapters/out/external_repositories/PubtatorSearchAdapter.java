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
package com.biopatternsg.infrastructure.adapters.out.external_repositories;

import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.ports.out.external_repositories.PubtatorSearchRepoWeb;
import com.biopatternsg.infrastructure.adapters.in.consumers.PubtatorResponseParser;
import com.biopatternsg.infrastructure.external_services.QueryPubtator;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@ApplicationScoped
public class PubtatorSearchAdapter implements PubtatorSearchRepoWeb {

    private final QueryPubtator queryPubtator;
    private final PubtatorResponseParser pubtatorResponseParser;

    public PubtatorSearchAdapter(
            QueryPubtator queryPubtator,
            PubtatorResponseParser pubtatorResponseParser
    ) {
        this.queryPubtator = queryPubtator;
        this.pubtatorResponseParser = pubtatorResponseParser;
    }

    @Override
    public List<PubtatorResult> search(List<String> pmids) {
        String responseJson = queryPubtator.search(pmids);
        try {
            return pubtatorResponseParser.parse(responseJson);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse PubTator response", e);
        }
    }
}

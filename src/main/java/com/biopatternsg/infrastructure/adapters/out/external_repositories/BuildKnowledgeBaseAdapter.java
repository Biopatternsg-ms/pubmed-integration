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

import com.biopatternsg.domain.model.KbEvent;
import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.ports.out.external_repositories.BuildKnowledgeBaseRepoWeb;
import com.biopatternsg.domain.ports.out.external_repositories.GenerateKbResult;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.GenerateKbResponse;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.PubTatorDocumentRequest;
import com.biopatternsg.infrastructure.clients.internal.BuildKnowledgeBaseHttpClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class BuildKnowledgeBaseAdapter implements BuildKnowledgeBaseRepoWeb {

    private final BuildKnowledgeBaseHttpClient httpClient;

    @Inject
    public BuildKnowledgeBaseAdapter(@RestClient BuildKnowledgeBaseHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public GenerateKbResult generateKnowledgeBase(
            String pipelineId,
            String pmid,
            String title,
            String text,
            List<PubtatorResult.PubtatorObject> objects,
            List<PubtatorResult.PubtatorEvent> events) {

        PubTatorDocumentRequest request = new PubTatorDocumentRequest(
                pipelineId,
                pmid,
                title,
                text,
                objects,
                events
        );

        GenerateKbResponse response = httpClient.generateKb(request);

        if (response == null) {
            return new GenerateKbResult(Collections.emptyList(), Collections.emptyMap());
        }

        List<KbEvent> domainEvents = Collections.emptyList();
        if (response.events() != null) {
            domainEvents = response.events().stream()
                    .filter(infraEvent -> infraEvent.event() != null)
                    .map(infraEvent -> new KbEvent(
                            pipelineId,
                            infraEvent.event().first(),
                            infraEvent.event().relation(),
                            infraEvent.event().second(),
                            infraEvent.pubmedIds()
                    ))
                    .collect(Collectors.toList());
        }

        return new GenerateKbResult(
                domainEvents,
                response.synonyms() != null ? response.synonyms() : Collections.emptyMap()
        );
    }
}

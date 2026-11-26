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
package com.biopatternsg.infrastructure.mapper;

import com.biopatternsg.domain.model.KbEvent;
import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.ports.out.external_repositories.GenerateKbResult;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.GenerateKbResponse;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.PubTatorDocumentRequest;

import java.util.Collections;
import java.util.List;

/**
 * Stateless pure mapper for Build Knowledge Base requests and responses.
 */
public final class BuildKnowledgeBaseMapper {

    private BuildKnowledgeBaseMapper() {
    }

    public static PubTatorDocumentRequest modelToDocumentRequest(String pipelineId, PubtatorResult pubtatorResult) {
        if (pubtatorResult == null) {
            return null;
        }
        return new PubTatorDocumentRequest(
                pipelineId,
                pubtatorResult.pmid(),
                pubtatorResult.title(),
                pubtatorResult.text(),
                pubtatorResult.objects(),
                pubtatorResult.events()
        );
    }

    public static GenerateKbResult buildKbToModel(String pipelineId, GenerateKbResponse response) {
        if (response == null) {
            return new GenerateKbResult(Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        }

        List<KbEvent> domainEvents = response.events() == null
                ? Collections.emptyList()
                : response.events().stream()
                        .filter(infraEvent -> infraEvent.event() != null)
                        .map(infraEvent -> new KbEvent(
                                pipelineId,
                                infraEvent.event().first(),
                                infraEvent.event().relation(),
                                infraEvent.event().second(),
                                infraEvent.pubmedIds()
                        ))
                        .toList();

        return new GenerateKbResult(
                domainEvents,
                response.synonyms() != null ? response.synonyms() : Collections.emptyMap(),
                response.biotypes() != null ? response.biotypes() : Collections.emptyMap()
        );
    }
}

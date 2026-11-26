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
import com.biopatternsg.domain.ports.out.external_repositories.BuildKnowledgeBaseRepoWeb;
import com.biopatternsg.domain.ports.out.external_repositories.GenerateKbResult;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.GenerateKbResponse;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.PubTatorDocumentRequest;
import com.biopatternsg.infrastructure.clients.internal.BuildKnowledgeBaseHttpClient;
import com.biopatternsg.infrastructure.mapper.BuildKnowledgeBaseMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class BuildKnowledgeBaseAdapter implements BuildKnowledgeBaseRepoWeb {

    private final BuildKnowledgeBaseHttpClient httpClient;

    @Inject
    public BuildKnowledgeBaseAdapter(@RestClient BuildKnowledgeBaseHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public GenerateKbResult generateKnowledgeBase(String pipelineId, PubtatorResult pubtatorResult) {
        PubTatorDocumentRequest request = BuildKnowledgeBaseMapper.modelToDocumentRequest(pipelineId, pubtatorResult);
        GenerateKbResponse response = httpClient.generateKb(request);
        return BuildKnowledgeBaseMapper.buildKbToModel(pipelineId, response);
    }
}

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
package com.biopatternsg.application.usecase;

import com.biopatternsg.domain.model.PipelineSynonym;
import com.biopatternsg.domain.ports.in.GetSynonymsByName;
import com.biopatternsg.domain.ports.out.repositories.SynonymRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ApplicationScoped
public class GetSynonymsByNameUseCase implements GetSynonymsByName {

    private final SynonymRepository synonymRepository;

    public GetSynonymsByNameUseCase(SynonymRepository synonymRepository) {
        this.synonymRepository = synonymRepository;
    }

    @Override
    public Optional<PipelineSynonym> execute(String pipelineId, String name) {
        log.info("Executing GetSynonymsByNameUseCase for pipelineId=[{}], name=[{}]", pipelineId, name);
        return synonymRepository.findByPipelineIdAndName(pipelineId, name);
    }
}

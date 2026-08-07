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

import com.biopatternsg.domain.model.KbEvent;
import com.biopatternsg.domain.ports.in.GetKbEventsByTerm;
import com.biopatternsg.domain.ports.out.repositories.KbEventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
@ApplicationScoped
public class GetKbEventsByTermUseCase implements GetKbEventsByTerm {

    private final KbEventRepository kbEventRepository;

    public GetKbEventsByTermUseCase(KbEventRepository kbEventRepository) {
        this.kbEventRepository = kbEventRepository;
    }

    @Override
    public List<KbEvent> execute(String pipelineId, String term) {
        log.info("Executing GetKbEventsByTermUseCase for pipelineId=[{}], term=[{}]", pipelineId, term);
        if (term == null || term.isBlank()) {
            return Collections.emptyList();
        }
        return kbEventRepository.findByPipelineIdAndTerm(pipelineId, term);
    }
}

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

import com.biopatternsg.domain.model.KbObject;
import com.biopatternsg.domain.ports.in.GetKbObjectByName;
import com.biopatternsg.domain.ports.out.repositories.KbObjectRepository;
import com.biopatternsg.domain.util.LogSanitizer;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ApplicationScoped
public class GetKbObjectByNameUseCase implements GetKbObjectByName {

    private final KbObjectRepository kbObjectRepository;

    public GetKbObjectByNameUseCase(KbObjectRepository kbObjectRepository) {
        this.kbObjectRepository = kbObjectRepository;
    }

    @Override
    public Optional<KbObject> execute(String pipelineId, String name) {
        log.info("Executing GetKbObjectByNameUseCase for pipelineId=[{}], name=[{}]",
                LogSanitizer.sanitize(pipelineId), LogSanitizer.sanitize(name));
        return kbObjectRepository.findByPipelineIdAndName(pipelineId, name);
    }
}

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

import com.biopatternsg.domain.ports.in.ResetKbObjectRoles;
import com.biopatternsg.domain.ports.out.repositories.KbObjectRepository;
import com.biopatternsg.domain.util.LogSanitizer;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ResetKbObjectRolesUseCase implements ResetKbObjectRoles {

    private final KbObjectRepository kbObjectRepository;

    @Override
    public void execute(String pipelineId) {
        log.info("Resetting roles in kb_objects for pipelineId: {}", LogSanitizer.sanitize(pipelineId));
        if (pipelineId == null || pipelineId.isBlank()) {
            return;
        }
        kbObjectRepository.resetRoles(pipelineId);
    }
}

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
import com.biopatternsg.domain.model.RestrictionLevel;
import com.biopatternsg.domain.ports.in.GetKbEventsByRestriction;
import com.biopatternsg.domain.ports.out.repositories.KbEventRepository;
import com.biopatternsg.domain.util.LogSanitizer;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class GetKbEventsByRestrictionUseCase implements GetKbEventsByRestriction {

    private final KbEventRepository kbEventRepository;

    public GetKbEventsByRestrictionUseCase(KbEventRepository kbEventRepository) {
        this.kbEventRepository = kbEventRepository;
    }

    @Override
    public List<KbEvent> execute(String pipelineId, String restrictionLevel, List<String> alignedObjects) {
        log.info("Executing GetKbEventsByRestrictionUseCase for pipelineId=[{}], restrictionLevel=[{}], alignedObjectsCount=[{}]",
                LogSanitizer.sanitize(pipelineId),
                LogSanitizer.sanitize(restrictionLevel),
                alignedObjects != null ? alignedObjects.size() : 0);

        if (pipelineId == null || pipelineId.isBlank()) {
            return Collections.emptyList();
        }

        List<KbEvent> allEvents = kbEventRepository.findByPipelineId(pipelineId);
        if (allEvents.isEmpty()) {
            return List.of();
        }

        RestrictionLevel level = RestrictionLevel.fromString(restrictionLevel);
        if (level == RestrictionLevel.UNRESTRICTED) {
            return allEvents;
        }

        Set<String> alignedSet = normalizeAlignedObjects(alignedObjects);
        if (alignedSet.isEmpty()) {
            return List.of();
        }

        return allEvents.stream()
                .filter(event -> level.matches(event, alignedSet))
                .toList();
    }

    private Set<String> normalizeAlignedObjects(List<String> alignedObjects) {
        if (alignedObjects == null || alignedObjects.isEmpty()) {
            return Set.of();
        }
        return alignedObjects.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toSet());
    }
}

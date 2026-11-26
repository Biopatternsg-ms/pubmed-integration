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

import com.biopatternsg.application.services.ExpertObjectAlignmentService;
import com.biopatternsg.domain.exception.AlignmentException;
import com.biopatternsg.domain.model.AlignedResult;
import com.biopatternsg.domain.model.ExpertObjectConfig;
import com.biopatternsg.domain.model.PipelineStepUpdate;
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.in.GenerateAlignedObjects;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.repositories.AlignedResultRepository;
import com.biopatternsg.domain.ports.out.repositories.KbObjectRepository;
import com.biopatternsg.domain.util.LogSanitizer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class GenerateAlignedObjectsUseCase implements GenerateAlignedObjects {

    private final KbObjectRepository kbObjectRepository;
    private final AlignedResultRepository alignedResultRepository;
    private final ConfigAndControlRepository configAndControlRepository;
    private final ExpertObjectAlignmentService expertObjectAlignmentService;

    @Inject
    public GenerateAlignedObjectsUseCase(
            KbObjectRepository kbObjectRepository,
            AlignedResultRepository alignedResultRepository,
            ConfigAndControlRepository configAndControlRepository,
            ExpertObjectAlignmentService expertObjectAlignmentService
    ) {
        this.kbObjectRepository = kbObjectRepository;
        this.alignedResultRepository = alignedResultRepository;
        this.configAndControlRepository = configAndControlRepository;
        this.expertObjectAlignmentService = expertObjectAlignmentService;
    }

    @Override
    public void execute(String pipelineId, List<ExpertObjectConfig> expertObjects, String userId) {
        List<ExpertObjectConfig> safeExpertObjects = expertObjects != null ? expertObjects : Collections.emptyList();
        log.info("Starting expert objects alignment for pipelineId=[{}] with [{}] expert objects [{}]",
                LogSanitizer.sanitize(pipelineId), safeExpertObjects.size(), safeExpertObjects.stream().map(ExpertObjectConfig::symbol).toList());

        try {
            Map<String, List<String>> synonymsMap = kbObjectRepository.findAllSynonymsByPipelineId(pipelineId);
            Map<String, List<String>> safeSynonyms = synonymsMap != null ? synonymsMap : Collections.emptyMap();
            log.info("Fetched synonyms dictionary with [{}] entries for pipelineId=[{}]",
                    safeSynonyms.size(), LogSanitizer.sanitize(pipelineId));

            AlignedResult alignedResult = expertObjectAlignmentService.align(pipelineId, safeExpertObjects, safeSynonyms);
            alignedResultRepository.save(alignedResult);

            Map<String, String> metrics = Map.of(
                    "alignedObjects", String.valueOf(alignedResult.aligned().size()),
                    "notAlignedObjects", String.valueOf(alignedResult.noAligned().size())
            );

            configAndControlRepository.updateStep(PipelineStepUpdate.withMetrics(
                    pipelineId, PipelineSteps.GENERATE_ALIGNED_OBJECTS, Status.COMPLETED, userId, metrics));
            log.info("Successfully completed expert objects alignment for pipelineId=[{}]", LogSanitizer.sanitize(pipelineId));
        } catch (Exception e) {
            log.error("Fatal error during expert objects alignment for pipelineId=[{}]", LogSanitizer.sanitize(pipelineId), e);
            configAndControlRepository.updateStep(PipelineStepUpdate.of(
                    pipelineId, PipelineSteps.GENERATE_ALIGNED_OBJECTS, Status.FAILED, userId));
            throw new AlignmentException("Failed to generate aligned objects for pipeline " + pipelineId, e);
        }
    }
}

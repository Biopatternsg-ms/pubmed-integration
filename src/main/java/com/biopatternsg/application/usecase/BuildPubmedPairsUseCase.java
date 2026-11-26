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

import com.biopatternsg.application.services.PubmedPairsBuilderService;
import com.biopatternsg.domain.model.BuildPubmedPairsCommand;
import com.biopatternsg.domain.model.PipelinePairsBatch;
import com.biopatternsg.domain.model.PipelineStepUpdate;
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.model.TermPair;
import com.biopatternsg.domain.ports.in.BuildPubmedPairs;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@ApplicationScoped
public class BuildPubmedPairsUseCase implements BuildPubmedPairs {

    private static final int SECOND_LEVEL = 2;

    private final PubmedPairsBuilderService pubmedPairsBuilderService;
    private final PairRepository pairRepository;
    private final ConfigAndControlRepository configAndControlRepository;

    public BuildPubmedPairsUseCase(
            PubmedPairsBuilderService pubmedPairsBuilderService,
            PairRepository pairRepository,
            ConfigAndControlRepository configAndControlRepository) {
        this.pubmedPairsBuilderService = pubmedPairsBuilderService;
        this.pairRepository = pairRepository;
        this.configAndControlRepository = configAndControlRepository;
    }

    @Override
    public void execute(BuildPubmedPairsCommand command) {
        String pipelineId = command.pipelineId();
        boolean useOnlyPrincipalName = command.useShortName();
        int levels = command.levels();

        pairRepository.deleteByPipelineId(pipelineId);

        Set<TermPair> firstLevelPairs = pubmedPairsBuilderService.buildFirstLevelPairs(pipelineId, useOnlyPrincipalName);
        savePairs(firstLevelPairs, pipelineId);

        for (int currentLevel = SECOND_LEVEL; currentLevel <= levels; currentLevel++) {
            List<Set<TermPair>> batches = pubmedPairsBuilderService.buildHigherLevelPairBatches(
                    pipelineId, useOnlyPrincipalName, currentLevel);
            batches.forEach(batch -> savePairs(batch, pipelineId));
        }

        log.info("Generated unique pairs FINISHED");

        long totalPairs = pairRepository.findByPipelineId(pipelineId).stream()
                .filter(batch -> batch.pairs() != null)
                .mapToLong(batch -> batch.pairs().size())
                .sum();

        configAndControlRepository.updateStep(PipelineStepUpdate.withMetrics(
                pipelineId,
                PipelineSteps.COMBINATIONS,
                Status.COMPLETED,
                command.userId(),
                Map.of("combinationsGenerated", String.valueOf(totalPairs))));
    }

    private void savePairs(Set<TermPair> pairs, String pipelineId) {
        pairRepository.save(new PipelinePairsBatch(pipelineId, List.copyOf(pairs)));
    }
}

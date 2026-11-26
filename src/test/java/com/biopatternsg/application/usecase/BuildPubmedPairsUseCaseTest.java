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
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuildPubmedPairsUseCase - Orchestration")
class BuildPubmedPairsUseCaseTest {

    private static final String PIPELINE_ID = "pipeline-001";
    private static final String USER_ID = "user-001";

    @Mock
    private PubmedPairsBuilderService pubmedPairsBuilderService;
    @Mock
    private PairRepository pairRepository;
    @Mock
    private ConfigAndControlRepository configAndControlRepository;

    private BuildPubmedPairsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BuildPubmedPairsUseCase(pubmedPairsBuilderService, pairRepository, configAndControlRepository);
    }

    @Test
    void execute_orchestratesDeletionPairBuildingSavingAndStepCompletion() {
        Set<TermPair> level1 = Set.of(new TermPair("BRCA1", "TP53"));
        Set<TermPair> level2 = Set.of(new TermPair("TP53", "MDM2"), new TermPair("TP53", "CDKN1A"));

        when(pubmedPairsBuilderService.buildFirstLevelPairs(PIPELINE_ID, true)).thenReturn(level1);
        when(pubmedPairsBuilderService.buildHigherLevelPairBatches(PIPELINE_ID, true, 2)).thenReturn(List.of(level2));
        when(pairRepository.findByPipelineId(PIPELINE_ID)).thenReturn(List.of(
                new PipelinePairsBatch(PIPELINE_ID, List.copyOf(level1)),
                new PipelinePairsBatch(PIPELINE_ID, List.copyOf(level2))
        ));

        useCase.execute(new BuildPubmedPairsCommand(PIPELINE_ID, true, 2, USER_ID));

        verify(pairRepository).deleteByPipelineId(PIPELINE_ID);
        verify(pubmedPairsBuilderService).buildFirstLevelPairs(PIPELINE_ID, true);
        verify(pubmedPairsBuilderService).buildHigherLevelPairBatches(PIPELINE_ID, true, 2);
        verify(pairRepository, times(2)).save(any(PipelinePairsBatch.class));
        verify(configAndControlRepository).updateStep(PipelineStepUpdate.withMetrics(
                PIPELINE_ID,
                PipelineSteps.COMBINATIONS,
                Status.COMPLETED,
                USER_ID,
                Map.of("combinationsGenerated", "3")
        ));
    }
}

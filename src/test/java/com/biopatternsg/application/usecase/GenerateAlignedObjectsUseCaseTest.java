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
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.repositories.AlignedResultRepository;
import com.biopatternsg.domain.ports.out.repositories.KbObjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateAlignedObjectsUseCaseTest {

    @Mock
    private KbObjectRepository kbObjectRepository;

    @Mock
    private AlignedResultRepository alignedResultRepository;

    @Mock
    private ConfigAndControlRepository configAndControlRepository;

    @Mock
    private ExpertObjectAlignmentService expertObjectAlignmentService;

    @InjectMocks
    private GenerateAlignedObjectsUseCase generateAlignedObjectsUseCase;

    private final String pipelineId = "pipe-123";
    private final String userId = "user-456";

    @Test
    void execute_ShouldAlignObjectsAndNotifyCompleted_WhenSuccess() {
        List<ExpertObjectConfig> expertObjects = List.of(
                new ExpertObjectConfig("P04637", "HGNC:11998", "TP53"),
                new ExpertObjectConfig("P01116", "HGNC:6407", "KRAS")
        );
        Map<String, List<String>> synonymsMap = Map.of("TP53", List.of("TP53", "p53"));
        AlignedResult alignedResult = new AlignedResult(
                pipelineId, List.of("TP53"), List.of("KRAS"), List.of(), List.of("TP53"));

        when(kbObjectRepository.findAllSynonymsByPipelineId(pipelineId)).thenReturn(synonymsMap);
        when(expertObjectAlignmentService.align(pipelineId, expertObjects, synonymsMap)).thenReturn(alignedResult);

        generateAlignedObjectsUseCase.execute(pipelineId, expertObjects, userId);

        verify(alignedResultRepository).save(alignedResult);
        verify(configAndControlRepository).updateStep(PipelineStepUpdate.withMetrics(
                pipelineId,
                PipelineSteps.GENERATE_ALIGNED_OBJECTS,
                Status.COMPLETED,
                userId,
                Map.of("alignedObjects", "1", "notAlignedObjects", "1")
        ));
    }

    @Test
    void execute_ShouldNotifyFailedStatus_WhenExceptionOccurs() {
        List<ExpertObjectConfig> expertObjects = List.of(
                new ExpertObjectConfig("P04637", "HGNC:11998", "TP53")
        );
        when(kbObjectRepository.findAllSynonymsByPipelineId(pipelineId))
                .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> generateAlignedObjectsUseCase.execute(pipelineId, expertObjects, userId))
                .isInstanceOf(AlignmentException.class);

        verify(configAndControlRepository).updateStep(PipelineStepUpdate.of(
                pipelineId, PipelineSteps.GENERATE_ALIGNED_OBJECTS, Status.FAILED, userId));
    }
}

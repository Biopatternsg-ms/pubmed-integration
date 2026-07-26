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

import com.biopatternsg.domain.model.AlignedResult;
import com.biopatternsg.domain.model.ExpertObjectConfig;
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.repositories.AlignedResultRepository;
import com.biopatternsg.domain.ports.out.repositories.SynonymRepository;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateAlignedObjectsUseCaseTest {

    @Mock
    private SynonymRepository synonymRepository;

    @Mock
    private AlignedResultRepository alignedResultRepository;

    @Mock
    private ConfigAndControlRepository configAndControlRepository;

    @InjectMocks
    private GenerateAlignedObjectsUseCase generateAlignedObjectsUseCase;

    private final String pipelineId = "pipe-123";
    private final String userId = "user-456";

    @BeforeEach
    void setUp() {
    }

    @Test
    void execute_ShouldAlignObjectsCorrectly_WhenExpertObjectsAndSynonymsExist() {
        // Arrange
        List<ExpertObjectConfig> expertObjects = List.of(
                new ExpertObjectConfig("P04637", "HGNC:11998", "TP53"),
                new ExpertObjectConfig("P38398", "HGNC:1100", "BRCA1"),
                new ExpertObjectConfig("P01116", "HGNC:6407", "KRAS")
        );

        Map<String, List<String>> synonymsMap = Map.of(
                "TP53", List.of("TP53", "p53", "P53_HUMAN"),
                "BRCA1", List.of("BRCA1")
        );

        when(synonymRepository.findAllByPipelineId(pipelineId)).thenReturn(synonymsMap);

        // Act
        generateAlignedObjectsUseCase.execute(pipelineId, expertObjects, userId);

        // Assert
        ArgumentCaptor<AlignedResult> captor = ArgumentCaptor.forClass(AlignedResult.class);
        verify(alignedResultRepository).save(captor.capture());
        AlignedResult savedResult = captor.getValue();

        assertThat(savedResult.pipelineId()).isEqualTo(pipelineId);
        assertThat(savedResult.aligned()).containsExactlyInAnyOrder("TP53", "BRCA1");
        assertThat(savedResult.noAligned()).containsExactlyInAnyOrder("KRAS");

        verify(configAndControlRepository).updateStep(pipelineId, PipelineSteps.GENERATE_ALIGNED_OBJECTS, Status.COMPLETED, userId);
    }

    @Test
    void execute_ShouldNotifyFailedStatus_WhenExceptionOccurs() {
        // Arrange
        List<ExpertObjectConfig> expertObjects = List.of(
                new ExpertObjectConfig("P04637", "HGNC:11998", "TP53")
        );
        when(synonymRepository.findAllByPipelineId(pipelineId))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> generateAlignedObjectsUseCase.execute(pipelineId, expertObjects, userId))
                .isInstanceOf(InternalServerError.class);

        verify(configAndControlRepository).updateStep(pipelineId, PipelineSteps.GENERATE_ALIGNED_OBJECTS, Status.FAILED, userId);
    }
}

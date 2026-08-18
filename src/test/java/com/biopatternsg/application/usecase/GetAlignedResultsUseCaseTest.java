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

import com.biopatternsg.domain.model.AlignedAs;
import com.biopatternsg.domain.model.AlignedResultSummary;
import com.biopatternsg.domain.ports.out.repositories.AlignedResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAlignedResultsUseCaseTest {

    @Mock
    private AlignedResultRepository alignedResultRepository;

    @InjectMocks
    private GetAlignedResultsUseCase useCase;

    private final String pipelineId = "pipe-123";

    @Test
    void execute_ShouldReturnAlignedResultSummary_WhenRepositoryHasData() {
        // Arrange
        AlignedResultSummary expectedSummary = new AlignedResultSummary(
                pipelineId,
                List.of("TP53", "BRCA1"),
                List.of("KRAS"),
                List.of(new AlignedAs("TP53", List.of("p53")))
        );
        when(alignedResultRepository.findByPipelineId(pipelineId)).thenReturn(Optional.of(expectedSummary));

        // Act
        Optional<AlignedResultSummary> result = useCase.execute(pipelineId);

        // Assert
        assertThat(result).isPresent().contains(expectedSummary);
        verify(alignedResultRepository).findByPipelineId(pipelineId);
    }

    @Test
    void execute_ShouldReturnEmptyOptional_WhenRepositoryHasNoData() {
        // Arrange
        when(alignedResultRepository.findByPipelineId(pipelineId)).thenReturn(Optional.empty());

        // Act
        Optional<AlignedResultSummary> result = useCase.execute(pipelineId);

        // Assert
        assertThat(result).isEmpty();
        verify(alignedResultRepository).findByPipelineId(pipelineId);
    }
}

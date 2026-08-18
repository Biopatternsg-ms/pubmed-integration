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

import com.biopatternsg.domain.model.PipelineSynonym;
import com.biopatternsg.domain.ports.out.repositories.SynonymRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetSynonymsByNameUseCaseTest {

    @Mock
    private SynonymRepository synonymRepository;

    private GetSynonymsByNameUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetSynonymsByNameUseCase(synonymRepository);
    }

    @Test
    void execute_returnsSynonymWhenFound() {
        String pipelineId = "pipe-123";
        String name = "CYP7A1";
        PipelineSynonym expected = new PipelineSynonym("CYP7A1", List.of("CYP7A1", "LOC101790267"));

        when(synonymRepository.findByPipelineIdAndName(pipelineId, name)).thenReturn(Optional.of(expected));

        Optional<PipelineSynonym> result = useCase.execute(pipelineId, name);

        assertTrue(result.isPresent());
        assertEquals("CYP7A1", result.get().name());
        assertEquals(2, result.get().synonyms().size());
        verify(synonymRepository).findByPipelineIdAndName(pipelineId, name);
    }

    @Test
    void execute_returnsEmptyWhenNotFound() {
        String pipelineId = "pipe-123";
        String name = "UNKNOWN";

        when(synonymRepository.findByPipelineIdAndName(pipelineId, name)).thenReturn(Optional.empty());

        Optional<PipelineSynonym> result = useCase.execute(pipelineId, name);

        assertTrue(result.isEmpty());
        verify(synonymRepository).findByPipelineIdAndName(pipelineId, name);
    }
}

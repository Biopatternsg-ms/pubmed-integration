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
import com.biopatternsg.domain.ports.out.repositories.KbObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetKbObjectByNameUseCaseTest {

    @Mock
    private KbObjectRepository kbObjectRepository;

    private GetKbObjectByNameUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetKbObjectByNameUseCase(kbObjectRepository);
    }

    @Test
    void execute_returnsKbObjectWhenFound() {
        String pipelineId = "pipe-123";
        String name = "CYP7A1";
        KbObject expected = new KbObject("CYP7A1", List.of("CYP7A1", "LOC101790267"));

        when(kbObjectRepository.findByPipelineIdAndName(pipelineId, name)).thenReturn(Optional.of(expected));

        Optional<KbObject> result = useCase.execute(pipelineId, name);

        assertTrue(result.isPresent());
        assertEquals("CYP7A1", result.get().name());
        assertEquals(2, result.get().synonyms().size());
        verify(kbObjectRepository).findByPipelineIdAndName(pipelineId, name);
    }

    @Test
    void execute_returnsEmptyWhenNotFound() {
        String pipelineId = "pipe-123";
        String name = "UNKNOWN";

        when(kbObjectRepository.findByPipelineIdAndName(pipelineId, name)).thenReturn(Optional.empty());

        Optional<KbObject> result = useCase.execute(pipelineId, name);

        assertTrue(result.isEmpty());
        verify(kbObjectRepository).findByPipelineIdAndName(pipelineId, name);
    }
}

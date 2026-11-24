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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetAllKbObjectsByPipelineUseCaseTest {

    @Mock
    private KbObjectRepository kbObjectRepository;

    private GetAllKbObjectsByPipelineUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetAllKbObjectsByPipelineUseCase(kbObjectRepository);
    }

    @Test
    void execute_returnsObjectsWhenFound() {
        String pipelineId = "pipe-123";
        KbObject obj1 = new KbObject("TP53", List.of("P53"), List.of("gene"), List.of("PROTEIN"));
        KbObject obj2 = new KbObject("BRCA1", List.of("BRCAI"), List.of("gene"), List.of("PROTEIN", "ENZYME"));

        when(kbObjectRepository.findAllByPipelineId(pipelineId)).thenReturn(List.of(obj1, obj2));

        List<KbObject> result = useCase.execute(pipelineId);

        assertEquals(2, result.size());
        assertEquals("TP53", result.get(0).name());
        assertEquals(List.of("PROTEIN"), result.get(0).roles());
        assertEquals("BRCA1", result.get(1).name());
        verify(kbObjectRepository).findAllByPipelineId(pipelineId);
    }

    @Test
    void execute_returnsEmptyListWhenPipelineIdIsNull() {
        List<KbObject> result = useCase.execute(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void execute_returnsEmptyListWhenPipelineIdIsBlank() {
        List<KbObject> result = useCase.execute("   ");

        assertTrue(result.isEmpty());
    }
}

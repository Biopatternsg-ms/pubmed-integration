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
import com.biopatternsg.domain.ports.out.repositories.KbEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetKbEventsByPipelineUseCaseTest {

    @Mock
    private KbEventRepository kbEventRepository;

    private GetKbEventsByPipelineUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetKbEventsByPipelineUseCase(kbEventRepository);
    }

    @Test
    void execute_returnsEventsWhenFound() {
        String pipelineId = "pipe-123";
        KbEvent event1 = new KbEvent(pipelineId, "TP53", "binds", "MDM2", List.of("PMID:12345"));
        KbEvent event2 = new KbEvent(pipelineId, "BRCA1", "interacts_with", "BARD1", List.of("PMID:67890"));

        when(kbEventRepository.findByPipelineId(pipelineId)).thenReturn(List.of(event1, event2));

        List<KbEvent> result = useCase.execute(pipelineId);

        assertEquals(2, result.size());
        assertEquals("TP53", result.get(0).first());
        assertEquals("BRCA1", result.get(1).first());
        verify(kbEventRepository).findByPipelineId(pipelineId);
    }

    @Test
    void execute_returnsEmptyListWhenPipelineIdIsNull() {
        List<KbEvent> result = useCase.execute(null);

        assertTrue(result.isEmpty());
        verifyNoInteractions(kbEventRepository);
    }

    @Test
    void execute_returnsEmptyListWhenPipelineIdIsBlank() {
        List<KbEvent> result = useCase.execute("   ");

        assertTrue(result.isEmpty());
        verifyNoInteractions(kbEventRepository);
    }
}

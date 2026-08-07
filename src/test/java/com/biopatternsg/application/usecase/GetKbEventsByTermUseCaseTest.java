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

class GetKbEventsByTermUseCaseTest {

    @Mock
    private KbEventRepository kbEventRepository;

    private GetKbEventsByTermUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetKbEventsByTermUseCase(kbEventRepository);
    }

    @Test
    void execute_returnsEventsWhenFound() {
        String pipelineId = "pipe-123";
        String term = "TP53";
        KbEvent event = new KbEvent(pipelineId, "TP53", "binds", "MDM2", List.of("PMID:12345"));

        when(kbEventRepository.findByPipelineIdAndTerm(pipelineId, term)).thenReturn(List.of(event));

        List<KbEvent> result = useCase.execute(pipelineId, term);

        assertEquals(1, result.size());
        assertEquals("TP53", result.get(0).first());
        assertEquals("binds", result.get(0).relation());
        assertEquals("MDM2", result.get(0).second());
        verify(kbEventRepository).findByPipelineIdAndTerm(pipelineId, term);
    }

    @Test
    void execute_returnsEmptyListWhenTermIsBlank() {
        List<KbEvent> result = useCase.execute("pipe-123", "   ");
        assertTrue(result.isEmpty());
        verifyNoInteractions(kbEventRepository);
    }
}

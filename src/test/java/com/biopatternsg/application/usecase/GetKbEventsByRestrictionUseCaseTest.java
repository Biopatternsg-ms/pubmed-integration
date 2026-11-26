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

class GetKbEventsByRestrictionUseCaseTest {

    @Mock
    private KbEventRepository kbEventRepository;

    private GetKbEventsByRestrictionUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetKbEventsByRestrictionUseCase(kbEventRepository);
    }

    @Test
    void execute_veryRestricted_requiresBothEndpoints() {
        String pipelineId = "pipe-123";
        KbEvent ev1 = new KbEvent(pipelineId, "TP53", "binds", "MDM2", List.of("1"));
        KbEvent ev2 = new KbEvent(pipelineId, "TP53", "activates", "CDKN1A", List.of("2"));
        KbEvent ev3 = new KbEvent(pipelineId, "EGFR", "phosphorylates", "AKT1", List.of("3"));

        when(kbEventRepository.findByPipelineId(pipelineId)).thenReturn(List.of(ev1, ev2, ev3));

        List<KbEvent> result = useCase.execute(pipelineId, "VERY_RESTRICTED", List.of("tp53", "mdm2"));

        assertEquals(1, result.size());
        assertEquals(ev1, result.get(0));
        verify(kbEventRepository).findByPipelineId(pipelineId);
    }

    @Test
    void execute_restricted_requiresAtLeastOneEndpoint() {
        String pipelineId = "pipe-123";
        KbEvent ev1 = new KbEvent(pipelineId, "TP53", "binds", "MDM2", List.of("1"));
        KbEvent ev2 = new KbEvent(pipelineId, "TP53", "activates", "CDKN1A", List.of("2"));
        KbEvent ev3 = new KbEvent(pipelineId, "EGFR", "phosphorylates", "AKT1", List.of("3"));

        when(kbEventRepository.findByPipelineId(pipelineId)).thenReturn(List.of(ev1, ev2, ev3));

        List<KbEvent> result = useCase.execute(pipelineId, "RESTRICTED", List.of("tp53"));

        assertEquals(2, result.size());
        assertTrue(result.contains(ev1));
        assertTrue(result.contains(ev2));
        assertFalse(result.contains(ev3));
    }

    @Test
    void execute_unrestrictedAndNoRestrictedAndBlank_returnsAllEvents() {
        String pipelineId = "pipe-123";
        KbEvent ev1 = new KbEvent(pipelineId, "TP53", "binds", "MDM2", List.of("1"));
        KbEvent ev2 = new KbEvent(pipelineId, "EGFR", "phosphorylates", "AKT1", List.of("2"));

        when(kbEventRepository.findByPipelineId(pipelineId)).thenReturn(List.of(ev1, ev2));

        assertEquals(2, useCase.execute(pipelineId, "UNRESTRICTED", List.of("TP53")).size());
        assertEquals(2, useCase.execute(pipelineId, "NO_RESTRICTED", List.of("TP53")).size());
        assertEquals(2, useCase.execute(pipelineId, null, List.of("TP53")).size());
        assertEquals(2, useCase.execute(pipelineId, "  ", List.of("TP53")).size());
    }

    @Test
    void execute_emptyAlignedObjectsForRestricted_returnsEmpty() {
        String pipelineId = "pipe-123";
        KbEvent ev1 = new KbEvent(pipelineId, "TP53", "binds", "MDM2", List.of("1"));
        when(kbEventRepository.findByPipelineId(pipelineId)).thenReturn(List.of(ev1));

        assertTrue(useCase.execute(pipelineId, "VERY_RESTRICTED", List.of()).isEmpty());
        assertTrue(useCase.execute(pipelineId, "RESTRICTED", null).isEmpty());
    }

    @Test
    void execute_returnsEmptyListWhenPipelineIdIsNullOrBlank() {
        assertTrue(useCase.execute(null, "RESTRICTED", List.of("TP53")).isEmpty());
        assertTrue(useCase.execute("   ", "RESTRICTED", List.of("TP53")).isEmpty());
        verifyNoInteractions(kbEventRepository);
    }
}

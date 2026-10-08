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

import com.biopatternsg.domain.ports.out.repositories.KbObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UpdateKbObjectRolesUseCaseTest {

    @Mock
    private KbObjectRepository kbObjectRepository;

    private UpdateKbObjectRolesUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new UpdateKbObjectRolesUseCase(kbObjectRepository);
    }

    @Test
    void execute_callsRepositoryWhenValid() {
        String pipelineId = "pipe-123";
        Map<String, List<String>> rolesMap = Map.of(
                "TP53", List.of("PROTEIN", "TRANSCRIPTION_FACTOR"),
                "BRCA1", List.of("PROTEIN")
        );

        useCase.execute(pipelineId, rolesMap);

        verify(kbObjectRepository).updateRoles(pipelineId, rolesMap);
    }

    @Test
    void execute_doesNothingWhenPipelineIdIsNull() {
        useCase.execute(null, Map.of("TP53", List.of("PROTEIN")));

        verify(kbObjectRepository, never()).updateRoles(null, Map.of("TP53", List.of("PROTEIN")));
    }

    @Test
    void execute_doesNothingWhenRolesMapIsNull() {
        useCase.execute("pipe-123", null);

        verify(kbObjectRepository, never()).updateRoles("pipe-123", null);
    }

    @Test
    void execute_doesNothingWhenRolesMapIsEmpty() {
        useCase.execute("pipe-123", Collections.emptyMap());

        verify(kbObjectRepository, never()).updateRoles("pipe-123", Collections.emptyMap());
    }
}

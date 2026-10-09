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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ResetKbObjectRolesUseCaseTest {

    @Mock
    private KbObjectRepository kbObjectRepository;

    private ResetKbObjectRolesUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new ResetKbObjectRolesUseCase(kbObjectRepository);
    }

    @Test
    void execute_callsRepositoryWhenValid() {
        String pipelineId = "pipe-123";

        useCase.execute(pipelineId);

        verify(kbObjectRepository).resetRoles(pipelineId);
    }

    @Test
    void execute_doesNothingWhenPipelineIdIsNull() {
        useCase.execute(null);

        verify(kbObjectRepository, never()).resetRoles(anyString());
    }

    @Test
    void execute_doesNothingWhenPipelineIdIsBlank() {
        useCase.execute("   ");

        verify(kbObjectRepository, never()).resetRoles(anyString());
    }
}

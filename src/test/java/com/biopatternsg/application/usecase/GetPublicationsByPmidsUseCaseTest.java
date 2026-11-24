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

import com.biopatternsg.domain.model.Publication;
import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetPublicationsByPmidsUseCaseTest {

    @Mock
    private PubtatorResultRepository pubtatorResultRepository;

    private GetPublicationsByPmidsUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetPublicationsByPmidsUseCase(pubtatorResultRepository);
    }

    @Test
    void execute_returnsPublicationsWhenFound() {
        List<String> pmids = List.of("10523821", "10523821", " 12345678 ");
        PubtatorResult r1 = new PubtatorResult("10523821", "Title 1", "Abstract text 1", Collections.emptyList(), Collections.emptyList());
        PubtatorResult r2 = new PubtatorResult("12345678", "Title 2", "Abstract text 2", Collections.emptyList(), Collections.emptyList());

        when(pubtatorResultRepository.findByPmids(List.of("10523821", "12345678"))).thenReturn(List.of(r1, r2));

        List<Publication> result = useCase.execute(pmids);

        assertEquals(2, result.size());
        assertEquals("10523821", result.get(0).pmid());
        assertEquals("Title 1", result.get(0).title());
        assertEquals("Abstract text 1", result.get(0).text());
        assertEquals("12345678", result.get(1).pmid());
        verify(pubtatorResultRepository).findByPmids(List.of("10523821", "12345678"));
    }

    @Test
    void execute_returnsEmptyListWhenInputIsEmptyOrNull() {
        assertTrue(useCase.execute(null).isEmpty());
        assertTrue(useCase.execute(Collections.emptyList()).isEmpty());
        assertTrue(useCase.execute(List.of("   ")).isEmpty());
        verifyNoInteractions(pubtatorResultRepository);
    }
}

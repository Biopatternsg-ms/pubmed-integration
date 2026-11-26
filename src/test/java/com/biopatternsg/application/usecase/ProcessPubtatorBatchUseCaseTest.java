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

import com.biopatternsg.domain.model.PipelineStepUpdate;
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.model.PubtatorSearchRequest;
import com.biopatternsg.domain.model.SearchProgress;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.external_repositories.PubtatorSearchRepoWeb;
import com.biopatternsg.domain.ports.out.repositories.PubtatorPmidReaderRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorSearchProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPubtatorBatchUseCaseTest {

    @Mock private PubtatorSearchRepoWeb pubtatorSearchRepoWeb;
    @Mock private PubtatorResultRepository pubtatorResultRepository;
    @Mock private ConfigAndControlRepository configAndControlRepository;
    @Mock private PubtatorSearchProgressRepository pubtatorSearchProgressRepository;
    @Mock private PubtatorPmidReaderRepository pubtatorPmidReaderRepository;

    @InjectMocks
    private ProcessPubtatorBatchUseCase useCase;

    @Test
    void execute_filtersCachedPmidsAndSavesOnlyDocsWithEvents() {
        PubtatorSearchRequest request = new PubtatorSearchRequest("pipe-1", List.of("111", "222", "333"), 1, 2, "user-1");
        PubtatorResult withEvents = new PubtatorResult(
                "222", "Title", "Text", List.of(),
                List.of(new PubtatorResult.PubtatorEvent("Positive_regulation", "TP53", "BRCA1"))
        );
        PubtatorResult withoutEvents = new PubtatorResult("333", "Title2", "Text2", List.of(), List.of());

        when(pubtatorResultRepository.findExistingPmids(List.of("111", "222", "333")))
                .thenReturn(List.of("111"));
        when(pubtatorSearchRepoWeb.search(List.of("222", "333")))
                .thenReturn(List.of(withEvents, withoutEvents));
        when(pubtatorSearchProgressRepository.incrementAndGet("pipe-1"))
                .thenReturn(new SearchProgress("pipe-1", 1, 2, "user-1"));

        useCase.execute(request);

        verify(pubtatorResultRepository).saveAll(List.of(withEvents));
        verify(configAndControlRepository, never()).updateStep(any());
    }

    @Test
    void execute_whenCompleted_notifiesMetricsAndDeletesProgress() {
        PubtatorSearchRequest request = new PubtatorSearchRequest("pipe-1", List.of("111"), 2, 2, "user-1");
        when(pubtatorResultRepository.findExistingPmids(List.of("111"))).thenReturn(List.of("111"));
        when(pubtatorSearchProgressRepository.incrementAndGet("pipe-1"))
                .thenReturn(new SearchProgress("pipe-1", 2, 2, "user-1"));
        when(pubtatorPmidReaderRepository.findPubmedIdsByPipelineId("pipe-1"))
                .thenReturn(List.of("111", "222", "333"));
        when(pubtatorResultRepository.findExistingPmids(List.of("111", "222", "333")))
                .thenReturn(List.of("111", "222"));

        useCase.execute(request);

        verify(pubtatorSearchRepoWeb, never()).search(any());
        verify(configAndControlRepository).updateStep(PipelineStepUpdate.withMetrics(
                "pipe-1",
                PipelineSteps.SEARCH_PUBTATOR,
                Status.COMPLETED,
                "user-1",
                Map.of("pmidsAnnotated", "2", "pmidsNotFound", "1")
        ));
        verify(pubtatorSearchProgressRepository).deleteByPipelineId("pipe-1");
    }
}

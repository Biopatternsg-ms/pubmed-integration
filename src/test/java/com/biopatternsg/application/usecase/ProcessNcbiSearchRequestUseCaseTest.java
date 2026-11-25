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

import com.biopatternsg.domain.model.NcbiSearchRequest;
import com.biopatternsg.domain.model.NcbiSearchResult;
import com.biopatternsg.domain.model.PipelineStepUpdate;
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.SearchProgress;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.external_repositories.NcbiSearchRepoWeb;
import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import com.biopatternsg.domain.ports.out.repositories.PubmedResultRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorPmidReaderRepository;
import com.biopatternsg.domain.ports.out.repositories.SearchProgressRepository;
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
class ProcessNcbiSearchRequestUseCaseTest {

    @Mock private NcbiSearchRepoWeb ncbiSearchRepoWeb;
    @Mock private PubmedResultRepository pubmedResultRepository;
    @Mock private PairRepository pairRepository;
    @Mock private ConfigAndControlRepository configAndControlRepository;
    @Mock private SearchProgressRepository searchProgressRepository;
    @Mock private PubtatorPmidReaderRepository pubtatorPmidReaderRepository;

    @InjectMocks
    private ProcessNcbiSearchRequestUseCase useCase;

    @Test
    void execute_whenNotCompleted_savesPmidsAndDoesNotNotifyCompletion() {
        NcbiSearchRequest request = new NcbiSearchRequest("pipe-1", "TP53 AND BRCA1", 100, 1, 5, "user-1");
        when(ncbiSearchRepoWeb.search("TP53 AND BRCA1", 100))
                .thenReturn(new NcbiSearchResult("TP53 AND BRCA1", 2, List.of("111", "222")));
        when(searchProgressRepository.incrementAndGet("pipe-1"))
                .thenReturn(new SearchProgress("pipe-1", 1, 5, "user-1"));

        useCase.execute(request);

        verify(pubmedResultRepository).save("pipe-1", "111");
        verify(pubmedResultRepository).save("pipe-1", "222");
        verify(pairRepository, never()).deleteByPipelineId(any());
        verify(configAndControlRepository, never()).updateStep(any());
    }

    @Test
    void execute_whenCompleted_cleansPairsAndNotifiesStepCompletion() {
        NcbiSearchRequest request = new NcbiSearchRequest("pipe-1", "TP53 AND BRCA1", 100, 2, 2, "user-1");
        when(ncbiSearchRepoWeb.search("TP53 AND BRCA1", 100))
                .thenReturn(new NcbiSearchResult("TP53 AND BRCA1", 1, List.of("111")));
        when(searchProgressRepository.incrementAndGet("pipe-1"))
                .thenReturn(new SearchProgress("pipe-1", 2, 2, "user-1"));
        when(pubtatorPmidReaderRepository.findPubmedIdsByPipelineId("pipe-1"))
                .thenReturn(List.of("111", "111", "222", "bad-id"));

        useCase.execute(request);

        verify(pairRepository).deleteByPipelineId("pipe-1");
        verify(configAndControlRepository).updateStep(PipelineStepUpdate.withMetrics(
                "pipe-1",
                PipelineSteps.SEARCH_PUBMED_IDS,
                Status.COMPLETED,
                "user-1",
                Map.of("pubmedIdsFound", "2", "pairsSearched", "2")
        ));
        verify(searchProgressRepository).deleteByPipelineId("pipe-1");
    }

    @Test
    void execute_whenSearchThrows_stillIncrementsAndReportsCompletion() {
        NcbiSearchRequest request = new NcbiSearchRequest("pipe-1", "TP53", 50, 1, 1, "user-1");
        when(ncbiSearchRepoWeb.search("TP53", 50)).thenThrow(new RuntimeException("NCBI down"));
        when(searchProgressRepository.incrementAndGet("pipe-1"))
                .thenReturn(new SearchProgress("pipe-1", 1, 1, "user-1"));
        when(pubtatorPmidReaderRepository.findPubmedIdsByPipelineId("pipe-1")).thenReturn(List.of());

        useCase.execute(request);

        verify(pairRepository).deleteByPipelineId("pipe-1");
        verify(configAndControlRepository).updateStep(PipelineStepUpdate.withMetrics(
                "pipe-1",
                PipelineSteps.SEARCH_PUBMED_IDS,
                Status.COMPLETED,
                "user-1",
                Map.of("pubmedIdsFound", "0", "pairsSearched", "1")
        ));
    }
}

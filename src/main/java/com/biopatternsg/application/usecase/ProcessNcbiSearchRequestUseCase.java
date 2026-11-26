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
import com.biopatternsg.domain.ports.in.ProcessNcbiSearchRequest;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.external_repositories.NcbiSearchRepoWeb;
import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import com.biopatternsg.domain.ports.out.repositories.PubmedResultRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorPmidReaderRepository;
import com.biopatternsg.domain.ports.out.repositories.SearchProgressRepository;
import com.biopatternsg.domain.util.LogSanitizer;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ProcessNcbiSearchRequestUseCase implements ProcessNcbiSearchRequest {

    private final NcbiSearchRepoWeb ncbiSearchRepoWeb;
    private final PubmedResultRepository pubmedResultRepository;
    private final PairRepository pairRepository;
    private final ConfigAndControlRepository configAndControlRepository;
    private final SearchProgressRepository searchProgressRepository;
    private final PubtatorPmidReaderRepository pubtatorPmidReaderRepository;

    @Override
    public void execute(NcbiSearchRequest request) {
        if (request == null || request.pipelineId() == null || request.pipelineId().isBlank()) {
            log.warn("Ignoring invalid NCBI search request: [{}]", LogSanitizer.sanitize(request));
            return;
        }

        try {
            NcbiSearchResult result = ncbiSearchRepoWeb.search(request.term(), request.retmax());
            log.info("NCBI search response: term=[{}] count=[{}] ids={}",
                    LogSanitizer.sanitize(result.term()), result.count(), LogSanitizer.sanitizeCollection(result.ids()));

            for (String pubmedId : result.ids()) {
                pubmedResultRepository.save(request.pipelineId(), pubmedId);
            }
        } catch (Exception e) {
            log.error("Error executing NCBI search for term=[{}]: {}",
                    LogSanitizer.sanitize(request.term()), LogSanitizer.sanitize(e.getMessage()), e);
        } finally {
            reportSearchProgress(request);
        }
    }

    private void reportSearchProgress(NcbiSearchRequest request) {
        SearchProgress progress = searchProgressRepository.incrementAndGet(request.pipelineId());
        if (progress == null || !progress.isCompleted()) {
            return;
        }

        pairRepository.deleteByPipelineId(request.pipelineId());
        log.info("Successfully deleted processed pairs for pipelineId=[{}] from database (all [{}] terms processed)",
                LogSanitizer.sanitize(request.pipelineId()), progress.totalCount());

        List<String> rawPmids = pubtatorPmidReaderRepository.findPubmedIdsByPipelineId(request.pipelineId());
        long uniquePmids = rawPmids != null
                ? rawPmids.stream().filter(id -> id != null && id.trim().matches("\\d+")).distinct().count()
                : 0;

        Map<String, String> metrics = Map.of(
                "pubmedIdsFound", String.valueOf(uniquePmids),
                "pairsSearched", String.valueOf(progress.totalCount())
        );

        configAndControlRepository.updateStep(PipelineStepUpdate.withMetrics(
                request.pipelineId(),
                PipelineSteps.SEARCH_PUBMED_IDS,
                Status.COMPLETED,
                request.userId(),
                metrics
        ));
        searchProgressRepository.deleteByPipelineId(request.pipelineId());
    }
}

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

import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.model.PubtatorSearchRequest;
import com.biopatternsg.domain.model.SearchProgress;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.in.ProcessPubtatorBatch;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.external_repositories.PubtatorSearchRepoWeb;
import com.biopatternsg.domain.ports.out.repositories.PubtatorPmidReaderRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorSearchProgressRepository;
import com.biopatternsg.domain.util.LogSanitizer;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ProcessPubtatorBatchUseCase implements ProcessPubtatorBatch {

    private final PubtatorSearchRepoWeb pubtatorSearchRepoWeb;
    private final PubtatorResultRepository pubtatorResultRepository;
    private final ConfigAndControlRepository configAndControlRepository;
    private final PubtatorSearchProgressRepository pubtatorSearchProgressRepository;
    private final PubtatorPmidReaderRepository pubtatorPmidReaderRepository;

    @Override
    public void execute(PubtatorSearchRequest request) {
        if (request == null || request.pipelineId() == null || request.pipelineId().isBlank()) {
            log.warn("Ignoring invalid PubTator batch request: [{}]", request);
            return;
        }

        List<String> pmids = request.pmids() != null ? request.pmids() : Collections.emptyList();

        try {
            List<String> pmidsToFetch = filterUncachedPmids(pmids);
            log.info("[{}/{}] PubTator processing batch of [{}] PMIDs (uncached: [{}]) for pipelineId=[{}]",
                    request.batchIndex(), request.batchesTotal(),
                    pmids.size(), pmidsToFetch.size(), LogSanitizer.sanitize(request.pipelineId()));

            if (!pmidsToFetch.isEmpty()) {
                List<PubtatorResult> docs = pubtatorSearchRepoWeb.search(pmidsToFetch);
                List<PubtatorResult> docsWithEvents = docs != null
                        ? docs.stream().filter(PubtatorResult::hasEvents).toList()
                        : Collections.emptyList();

                if (!docsWithEvents.isEmpty()) {
                    pubtatorResultRepository.saveAll(docsWithEvents);
                }
            }
        } catch (Exception e) {
            log.error("[{}/{}] Error processing PubTator batch for pipelineId=[{}]: {}",
                    request.batchIndex(), request.batchesTotal(),
                    LogSanitizer.sanitize(request.pipelineId()), e.getMessage(), e);
        } finally {
            reportBatchProgress(request);
        }
    }

    private List<String> filterUncachedPmids(List<String> pmids) {
        if (pmids.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> existingPmids = new HashSet<>(pubtatorResultRepository.findExistingPmids(pmids));
        return pmids.stream()
                .filter(pmid -> !existingPmids.contains(pmid))
                .toList();
    }

    private void reportBatchProgress(PubtatorSearchRequest request) {
        SearchProgress progress = pubtatorSearchProgressRepository.incrementAndGet(request.pipelineId());
        if (progress == null || !progress.isCompleted()) {
            return;
        }

        log.info("All PubTator batches completed for pipelineId=[{}] ([{}] batches)",
                LogSanitizer.sanitize(request.pipelineId()), progress.totalCount());

        List<String> rawPmids = pubtatorPmidReaderRepository.findPubmedIdsByPipelineId(request.pipelineId());
        int totalPmids = rawPmids != null ? rawPmids.size() : 0;
        List<String> existingInPubtator = totalPmids > 0
                ? pubtatorResultRepository.findExistingPmids(rawPmids)
                : Collections.emptyList();
        int annotatedPmids = existingInPubtator != null ? existingInPubtator.size() : 0;
        int notFoundPmids = Math.max(0, totalPmids - annotatedPmids);

        Map<String, String> metrics = Map.of(
                "pmidsAnnotated", String.valueOf(annotatedPmids),
                "pmidsNotFound", String.valueOf(notFoundPmids)
        );

        configAndControlRepository.updateStep(
                request.pipelineId(),
                PipelineSteps.SEARCH_PUBTATOR,
                Status.COMPLETED,
                request.userId(),
                metrics
        );
        pubtatorSearchProgressRepository.deleteByPipelineId(request.pipelineId());
    }
}

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
package com.biopatternsg.infrastructure.adapters.in.consumers;

import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.model.PubtatorSearchRequest;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.external_repositories.PubtatorSearchRepoWeb;
import com.biopatternsg.domain.ports.out.repositories.PubtatorPmidReaderRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorSearchProgressRepository;
import com.biopatternsg.mongo.PubtatorSearchProgressCollection;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class PubtatorRabbitConsumer {

    private final PubtatorSearchRepoWeb pubtatorSearchRepoWeb;
    private final PubtatorResultRepository pubtatorResultRepository;
    private final ConfigAndControlRepository configAndControlRepository;
    private final PubtatorSearchProgressRepository pubtatorSearchProgressRepository;
    private final PubtatorResponseParser pubtatorResponseParser;
    private final PubtatorPmidReaderRepository pubtatorPmidReaderRepository;

    @Incoming("pubtator-in")
    @Blocking(ordered = false)
    public void consume(JsonObject jsonMsg) {
        PubtatorSearchRequest request = jsonMsg.mapTo(PubtatorSearchRequest.class);

        try {
            List<String> pmidsToFetch = filterUncachedPmids(request.pmids());
            log.info("[{}/{}] PubTator processing batch of [{}] PMIDs (uncached: [{}]) for pipelineId=[{}]",
                    request.batchIndex(), request.batchesTotal(),
                    request.pmids().size(), pmidsToFetch.size(), request.pipelineId());

            if (!pmidsToFetch.isEmpty()) {
                String responseJson = pubtatorSearchRepoWeb.search(pmidsToFetch);
                List<PubtatorResult> docs = pubtatorResponseParser.parse(responseJson);
                List<PubtatorResult> docsWithEvents = docs.stream()
                        .filter(PubtatorResult::hasEvents)
                        .toList();

                if (!docsWithEvents.isEmpty()) {
                    pubtatorResultRepository.saveAll(docsWithEvents);
                }
            }
        } catch (Exception e) {
            log.error("[{}/{}] Error processing PubTator batch for pipelineId=[{}]: {}",
                    request.batchIndex(), request.batchesTotal(),
                    request.pipelineId(), e.getMessage(), e);
        } finally {
            reportBatchProgress(request);
        }
    }

    private List<String> filterUncachedPmids(List<String> pmids) {
        Set<String> existingPmids = new HashSet<>(pubtatorResultRepository.findExistingPmids(pmids));
        return pmids.stream()
                .filter(pmid -> !existingPmids.contains(pmid))
                .toList();
    }

    private void reportBatchProgress(PubtatorSearchRequest request) {
        PubtatorSearchProgressCollection progress =
                pubtatorSearchProgressRepository.incrementAndGet(request.pipelineId());

        if (progress != null && progress.getCompletedCount() == progress.getTotalCount()) {
            log.info("All PubTator batches completed for pipelineId=[{}] ([{}] batches)",
                    request.pipelineId(), progress.getTotalCount());

            List<String> rawPmids = pubtatorPmidReaderRepository.findPubmedIdsByPipelineId(request.pipelineId());
            int totalPmids = rawPmids != null ? rawPmids.size() : 0;
            List<String> existingInPubtator = totalPmids > 0 ? pubtatorResultRepository.findExistingPmids(rawPmids) : Collections.emptyList();
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
}

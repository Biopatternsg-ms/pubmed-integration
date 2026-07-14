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
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorSearchProgressRepository;
import com.biopatternsg.mongo.PubtatorSearchProgressCollection;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.HashSet;
import java.util.List;
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

    @Incoming("pubtator-in")
    @Blocking(ordered = false)
    public void consume(JsonObject jsonMsg) {
        PubtatorSearchRequest request = jsonMsg.mapTo(PubtatorSearchRequest.class);

        try {
            List<String> pmidsToFetch = filterUncachedPmids(request.pmids());

            if (!pmidsToFetch.isEmpty()) {
                log.info("[{}/{}] Fetching {} PMIDs from PubTator API (already cached: {})",
                        request.batchIndex(), request.batchesTotal(),
                        pmidsToFetch.size(), request.pmids().size() - pmidsToFetch.size());

                String responseJson = pubtatorSearchRepoWeb.search(pmidsToFetch);
                List<PubtatorResult> docs = pubtatorResponseParser.parse(responseJson);
                List<PubtatorResult> docsWithEvents = docs.stream()
                        .filter(PubtatorResult::hasEvents)
                        .toList();

                if (!docsWithEvents.isEmpty()) {
                    pubtatorResultRepository.saveAll(docsWithEvents);
                }
            } else {
                log.info("[{}/{}] All {} PMIDs already cached. Skipping API call.",
                        request.batchIndex(), request.batchesTotal(), request.pmids().size());
            }

            log.info("[{}/{}] PubTator batch processed successfully for pipelineId=[{}]",
                    request.batchIndex(), request.batchesTotal(), request.pipelineId());

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

            configAndControlRepository.updateStep(
                    request.pipelineId(),
                    PipelineSteps.SEARCH_PUBTATOR,
                    Status.COMPLETED,
                    request.userId()
            );
            pubtatorSearchProgressRepository.deleteByPipelineId(request.pipelineId());
        }
    }
}

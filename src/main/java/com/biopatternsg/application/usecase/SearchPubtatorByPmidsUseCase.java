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

import com.biopatternsg.domain.model.PubtatorSearchRequest;
import com.biopatternsg.domain.ports.in.SearchPubtatorByPmids;
import com.biopatternsg.domain.ports.out.producers.PubtatorQueueSender;
import com.biopatternsg.domain.ports.out.repositories.PubtatorPmidReaderRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorSearchProgressRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class SearchPubtatorByPmidsUseCase implements SearchPubtatorByPmids {

    private final PubtatorPmidReaderRepository pubtatorPmidReaderRepository;
    private final PubtatorQueueSender pubtatorQueueSender;
    private final PubtatorSearchProgressRepository pubtatorSearchProgressRepository;

    @ConfigProperty(name = "pubtator.batch-size")
    int batchSize;

    @Override
    public void execute(String pipelineId, String userId) {
        log.info("Starting PubTator enqueuing for pipelineId=[{}]", pipelineId);

        List<String> rawPmids = pubtatorPmidReaderRepository.findPubmedIdsByPipelineId(pipelineId);
        List<String> allPmids = rawPmids.stream()
                .filter(pmid -> pmid != null && pmid.trim().matches("\\d+"))
                .toList();
        log.info("Found [{}] unique PMIDs (raw: {}) for pipelineId=[{}]", allPmids.size(), rawPmids.size(), pipelineId);

        if (allPmids.isEmpty()) {
            log.warn("No valid numeric PMIDs found for pipelineId=[{}], nothing to enqueue.", pipelineId);
            return;
        }

        // Partition list into batches
        List<List<String>> batches = partitionList(allPmids, batchSize);
        int totalBatches = batches.size();

        log.info("Divided into [{}] batches of max [{}] PMIDs each", totalBatches, batchSize);

        // Initialize progress
        pubtatorSearchProgressRepository.initializeProgress(pipelineId, totalBatches, userId);

        // Enqueue each batch
        for (int i = 0; i < totalBatches; i++) {
            List<String> batch = batches.get(i);
            try {
                pubtatorQueueSender.send(
                        new PubtatorSearchRequest(pipelineId, batch, i + 1, totalBatches, userId)
                );
            } catch (Exception e) {
                log.error("[{}/{}] Error enqueuing PubTator batch: {}",
                        i + 1, totalBatches, e.getMessage(), e);
            }
        }

        log.info("PubTator enqueuing FINISHED for pipelineId=[{}]", pipelineId);
    }

    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}

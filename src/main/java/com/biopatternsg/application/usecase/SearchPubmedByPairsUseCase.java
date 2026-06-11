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
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.in.SearchPubmedByPairs;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.producers.NcbiQueueSender;
import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import com.biopatternsg.mongo.PairsCollection;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class SearchPubmedByPairsUseCase implements SearchPubmedByPairs {

    private final PairRepository pairRepository;
    private final NcbiQueueSender ncbiQueueSender;

    @Override
    public void execute(String pipelineId, int retmax, String userId) {
        log.info("Starting NCBI ESearch enqueuing for pipelineId=[{}] retmax=[{}]", pipelineId, retmax);

        List<PairsCollection> collections = pairRepository.findByPipelineId(pipelineId);

        List<PairsCollection.Pair> allPairs = collections.stream()
                .filter(c -> c.getPairs() != null)
                .flatMap(c -> c.getPairs().stream())
                .filter(Objects::nonNull)
                .toList();

        log.info("Found [{}] PairsCollection documents for pipelineId=[{}]", allPairs.size(), pipelineId);

        List<String> terms = allPairs.stream()
                .map(pair -> pair.getFirstTerm() + " AND " + pair.getSecondTerm())
                .toList();

        log.info("Total pairs to enqueue for NCBI: [{}]", terms.size());

        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i);
            try {
                ncbiQueueSender.send(new NcbiSearchRequest(pipelineId, term, retmax, i + 1, terms.size(), userId));
            } catch (Exception e) {
                log.error("[{}/{}] Error enqueuing NCBI request for term=[{}]: {}", i + 1, allPairs.size(), term, e.getMessage());
            }
        }

        log.info("NCBI ESearch enqueuing FINISHED for pipelineId=[{}]", pipelineId);
    }
}

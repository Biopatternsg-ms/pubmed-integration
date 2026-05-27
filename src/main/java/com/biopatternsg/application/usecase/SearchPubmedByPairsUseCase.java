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

import com.biopatternsg.domain.model.NcbiSearchResult;
import com.biopatternsg.domain.ports.in.SearchPubmedByPairs;
import com.biopatternsg.domain.ports.out.external_repositories.NcbiESearchPort;
import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import com.biopatternsg.mongo.PairsCollection;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class SearchPubmedByPairsUseCase implements SearchPubmedByPairs {

    private final PairRepository pairRepository;
    private final NcbiESearchPort ncbiESearchPort;

    private static final long DELAY_MS = 1000L;

    @Override
    public void execute(String pipelineId, int retmax) {
        log.info("Starting NCBI ESearch for pipelineId=[{}] retmax=[{}]", pipelineId, retmax);

        List<PairsCollection> collections = pairRepository.findByPipelineId(pipelineId);
        log.info("Found [{}] PairsCollection documents for pipelineId=[{}]", collections.size(), pipelineId);

        List<PairsCollection.Pair> allPairs = collections.stream()
                .filter(c -> c.getPairs() != null)
                .flatMap(c -> c.getPairs().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Total pairs to query in NCBI: [{}]", allPairs.size());

        for (int i = 0; i < allPairs.size(); i++) {
            PairsCollection.Pair pair = allPairs.get(i);
            String term = pair.getFirstTerm() + " AND " + pair.getSecondTerm();

            try {
                NcbiSearchResult result = ncbiESearchPort.search(term, retmax);
                log.info("[{}/{}] term=[{}] count=[{}] ids={}", i + 1, allPairs.size(), term, result.count(), result.ids());
            } catch (Exception e) {
                log.error("[{}/{}] Error querying NCBI for term=[{}]: {}", i + 1, allPairs.size(), term, e.getMessage());
            }

            if (i < allPairs.size() - 1) {
                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while waiting between NCBI queries");
                    break;
                }
            }
        }

        log.info("NCBI ESearch FINISHED for pipelineId=[{}]", pipelineId);
    }
}

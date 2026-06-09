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

import com.biopatternsg.domain.model.NcbiSearchRequest;
import com.biopatternsg.domain.model.NcbiSearchResult;
import com.biopatternsg.domain.ports.out.external_repositories.NcbiSearchRepoWeb;
import com.biopatternsg.infrastructure.external_services.QueryNcbiESearch;
import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import com.biopatternsg.domain.ports.out.repositories.PubmedResultRepository;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class NcbiQueueConsumerRabbitImpl {

    private final NcbiSearchRepoWeb ncbiSearchRepoWeb;
    private final PubmedResultRepository pubmedResultRepository;
    private final PairRepository pairRepository;

    private static final long DELAY_MS = 101L;

    @Incoming("ncbi-in")
    @Blocking
    public void consume(JsonObject jsonMsg) {
        NcbiSearchRequest request = jsonMsg.mapTo(NcbiSearchRequest.class);

        try {
            NcbiSearchResult result = ncbiSearchRepoWeb.search(request.term(), request.retmax());
            log.info("NCBI search response: term=[{}] count=[{}] ids={}", result.term(), result.count(), result.ids());

            for (String pubmedId : result.ids()) {
                pubmedResultRepository.save(request.pipelineId(), pubmedId);
            }
        } catch (Exception e) {
            log.error("Error executing NCBI search for term=[{}]: {}", request.term(), e.getMessage(), e);
        } finally {

            if (request.termIndex() == request.termsTotal()) {
                pairRepository.deleteByPipelineId(request.pipelineId());
                log.info("Successfully deleted processed pairs for pipelineId=[{}] from database", request.pipelineId());
            }

            try {
                Thread.sleep(DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Consumer rate limiting sleep interrupted", e);
            }
        }
    }
}

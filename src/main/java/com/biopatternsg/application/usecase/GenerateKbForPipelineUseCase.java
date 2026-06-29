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

import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.in.GenerateKbForPipeline;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.repositories.KbEventRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorPmidReaderRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.GenerateKbResponse;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.KbEvent;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.PubTatorDocumentRequest;
import com.biopatternsg.infrastructure.clients.internal.BuildKnowledgeBaseHttpClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ApplicationScoped
public class GenerateKbForPipelineUseCase implements GenerateKbForPipeline {

    private final PubtatorPmidReaderRepository pubtatorPmidReaderRepository;
    private final PubtatorResultRepository pubtatorResultRepository;
    private final BuildKnowledgeBaseHttpClient buildKnowledgeBaseHttpClient;
    private final KbEventRepository kbEventRepository;
    private final ConfigAndControlRepository configAndControlRepository;

    @Inject
    public GenerateKbForPipelineUseCase(
            PubtatorPmidReaderRepository pubtatorPmidReaderRepository,
            PubtatorResultRepository pubtatorResultRepository,
            @RestClient BuildKnowledgeBaseHttpClient buildKnowledgeBaseHttpClient,
            KbEventRepository kbEventRepository,
            ConfigAndControlRepository configAndControlRepository
    ) {
        this.pubtatorPmidReaderRepository = pubtatorPmidReaderRepository;
        this.pubtatorResultRepository = pubtatorResultRepository;
        this.buildKnowledgeBaseHttpClient = buildKnowledgeBaseHttpClient;
        this.kbEventRepository = kbEventRepository;
        this.configAndControlRepository = configAndControlRepository;
    }

    @Override
    public void execute(String pipelineId, String userId) {
        log.info("Starting knowledge base generation pipeline for pipelineId=[{}]", pipelineId);

        try {
            List<String> rawPmids = pubtatorPmidReaderRepository.findPubmedIdsByPipelineId(pipelineId);
            List<String> allPmids = rawPmids.stream()
                    .filter(pmid -> pmid != null && pmid.trim().matches("\\d+"))
                    .distinct()
                    .toList();

            log.info("Found [{}] unique numeric PMIDs to process for pipelineId=[{}]", allPmids.size(), pipelineId);

            if (allPmids.isEmpty()) {
                log.warn("No valid numeric PMIDs found for pipelineId=[{}]. Aborting KB generation.", pipelineId);
                configAndControlRepository.updateStep(pipelineId, PipelineSteps.BUILD_KNOWLEDGE_BASE, Status.COMPLETED, userId);
                return;
            }

            // 2. Procesar cada PMID en paralelo — operación I/O bound (HTTP + MongoDB)
            AtomicInteger successCount  = new AtomicInteger();
            AtomicInteger notFoundCount = new AtomicInteger();
            AtomicInteger errorCount    = new AtomicInteger();

            allPmids.parallelStream().forEach(pmid -> {
                try {
                    PubtatorResult pubtatorResult = pubtatorResultRepository.findByPmid(pmid);
                    if (pubtatorResult == null) {
                        log.warn("PubtatorResult not found in DB for PMID=[{}]", pmid);
                        notFoundCount.incrementAndGet();
                        return; // equivale a continue en el for secuencial
                    }

                    // 3. Consultar el endpoint generate-kb de build-knowledge-base
                    log.debug("Sending PMID=[{}] to build-knowledge-base endpoint", pmid);
                    var request = new PubTatorDocumentRequest(
                            pipelineId,
                            pubtatorResult.pmid(),
                            pubtatorResult.title(),
                            pubtatorResult.text(),
                            pubtatorResult.objects(),
                            pubtatorResult.events()
                    );
                    GenerateKbResponse kbResponse = buildKnowledgeBaseHttpClient.generateKb(request);
                    log.info("KB response received for PMID=[{}]: events=[{}], biotypes=[{}]",
                            pmid,
                            kbResponse.events() != null ? kbResponse.events().size() : 0,
                            kbResponse.biotypes() != null ? kbResponse.biotypes().size() : 0);

                    // 4. Persistir cada evento en MongoDB
                    if (kbResponse.events() != null) {
                        persistKbEvents(pipelineId, kbResponse.events());
                    }

                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("Error generating KB for PMID=[{}]: {}", pmid, e.getMessage(), e);
                    errorCount.incrementAndGet();
                }
            });

            log.info("Finished knowledge base generation pipeline for pipelineId=[{}]. Results: Success=[{}], NotFound=[{}], Errors=[{}]",
                    pipelineId, successCount.get(), notFoundCount.get(), errorCount.get());

            configAndControlRepository.updateStep(pipelineId, PipelineSteps.BUILD_KNOWLEDGE_BASE, Status.COMPLETED, userId);
        } catch (Exception e) {
            log.error("Fatal error during KB generation for pipelineId=[{}]", pipelineId, e);
            configAndControlRepository.updateStep(pipelineId, PipelineSteps.BUILD_KNOWLEDGE_BASE, Status.FAILED, userId);
        }
    }

    private void persistKbEvents(String pipelineId, List<KbEvent> kbEventDtos) {
        for (var kbEvent : kbEventDtos) {
            if (kbEvent.event() == null) {
                continue;
            }

            String first    = kbEvent.event().first();
            String relation = kbEvent.event().relation();
            String second   = kbEvent.event().second();

            try {
                kbEventRepository.upsert(pipelineId, first, relation, second, kbEvent.pubmedIds());
            } catch (Exception e) {
                log.error("Error persisting KbEvent [{}, [{},{},{}]]: {}", pipelineId, first, relation, second, e.getMessage(), e);
            }
        }
    }
}

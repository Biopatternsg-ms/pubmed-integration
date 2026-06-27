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

import com.biopatternsg.domain.model.KbEvent;
import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.ports.in.GenerateKbForPipeline;
import com.biopatternsg.domain.ports.out.repositories.KbEventRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorPmidReaderRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.GenerateKbResponse;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.PubTatorDocumentRequest;
import com.biopatternsg.infrastructure.clients.internal.BuildKnowledgeBaseHttpClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@Slf4j
@ApplicationScoped
public class GenerateKbForPipelineUseCase implements GenerateKbForPipeline {

    private final PubtatorPmidReaderRepository pubtatorPmidReaderRepository;
    private final PubtatorResultRepository pubtatorResultRepository;
    private final BuildKnowledgeBaseHttpClient buildKnowledgeBaseHttpClient;
    private final KbEventRepository kbEventRepository;

    @Inject
    public GenerateKbForPipelineUseCase(
            PubtatorPmidReaderRepository pubtatorPmidReaderRepository,
            PubtatorResultRepository pubtatorResultRepository,
            @RestClient BuildKnowledgeBaseHttpClient buildKnowledgeBaseHttpClient,
            KbEventRepository kbEventRepository
    ) {
        this.pubtatorPmidReaderRepository = pubtatorPmidReaderRepository;
        this.pubtatorResultRepository = pubtatorResultRepository;
        this.buildKnowledgeBaseHttpClient = buildKnowledgeBaseHttpClient;
        this.kbEventRepository = kbEventRepository;
    }

    @Override
    public void execute(String pipelineId, String userId) {
        log.info("Starting knowledge base generation pipeline for pipelineId=[{}]", pipelineId);

        List<String> rawPmids = pubtatorPmidReaderRepository.findPubmedIdsByPipelineId(pipelineId);
        List<String> allPmids = rawPmids.stream()
                .filter(pmid -> pmid != null && pmid.trim().matches("\\d+"))
                .distinct()
                .toList();

        log.info("Found [{}] unique numeric PMIDs to process for pipelineId=[{}]", allPmids.size(), pipelineId);

        if (allPmids.isEmpty()) {
            log.warn("No valid numeric PMIDs found for pipelineId=[{}]. Aborting KB generation.", pipelineId);
            return;
        }

        // 2. Recorrer uno a uno y obtener la info de pubtator_result
        int successCount = 0;
        int notFoundCount = 0;
        int errorCount = 0;

        for (String pmid : allPmids) {
            try {
                PubtatorResult pubtatorResult = pubtatorResultRepository.findByPmid(pmid);
                if (pubtatorResult == null) {
                    log.warn("PubtatorResult not found in DB for PMID=[{}]", pmid);
                    notFoundCount++;
                    continue;
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
                    persistKbEvents(kbResponse.events());
                }

                successCount++;

            } catch (Exception e) {
                log.error("Error generating KB for PMID=[{}]: {}", pmid, e.getMessage(), e);
                errorCount++;
            }
        }

        log.info("Finished knowledge base generation pipeline for pipelineId=[{}]. Results: Success=[{}], NotFound=[{}], Errors=[{}]",
                pipelineId, successCount, notFoundCount, errorCount);
    }

    private void persistKbEvents(List<com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.KbEvent> kbEventDtos) {
        for (var kbEvent : kbEventDtos) {
            if (kbEvent.event() == null) {
                continue;
            }

            String first    = kbEvent.event().first();
            String relation = kbEvent.event().relation();
            String second   = kbEvent.event().second();

            try {
                var existing = kbEventRepository.findByRelation(first, relation, second);

                if (existing.isEmpty()) {
                    // Evento nuevo: guardar completo con todos sus pubmedIds
                    kbEventRepository.save(new KbEvent(
                            first,
                            relation,
                            second,
                            kbEvent.pubmedIds() != null ? kbEvent.pubmedIds() : List.of()
                    ));
                    log.debug("New KbEvent created: [{},{},{}]", first, relation, second);
                } else {
                    // Evento existente: agregar solo los pubmedIds que no estén ya registrados
                    List<String> storedPubmedIds = existing.get().pubmedIds();
                    if (kbEvent.pubmedIds() != null) {
                        for (String pubmedId : kbEvent.pubmedIds()) {
                            if (!storedPubmedIds.contains(pubmedId)) {
                                kbEventRepository.addPubmedId(first, relation, second, pubmedId);
                                log.debug("PubmedId [{}] added to existing KbEvent [{},{},{}]",
                                        pubmedId, first, relation, second);
                            } else {
                                log.trace("PubmedId [{}] already registered in KbEvent [{},{},{}] — skipped",
                                        pubmedId, first, relation, second);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error persisting KbEvent [{},{},{}]: {}", first, relation, second, e.getMessage(), e);
            }
        }
    }
}

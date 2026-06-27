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
import com.biopatternsg.domain.ports.in.GenerateKbForPipeline;
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

    @Inject
    public GenerateKbForPipelineUseCase(
            PubtatorPmidReaderRepository pubtatorPmidReaderRepository,
            PubtatorResultRepository pubtatorResultRepository,
            @RestClient BuildKnowledgeBaseHttpClient buildKnowledgeBaseHttpClient
    ) {
        this.pubtatorPmidReaderRepository = pubtatorPmidReaderRepository;
        this.pubtatorResultRepository = pubtatorResultRepository;
        this.buildKnowledgeBaseHttpClient = buildKnowledgeBaseHttpClient;
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

        List<String> temporalTest = allPmids.subList(0, 1);

        for (String pmid : temporalTest) {
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
                successCount++;

            } catch (Exception e) {
                log.error("Error generating KB for PMID=[{}]: {}", pmid, e.getMessage(), e);
                errorCount++;
            }
        }

        log.info("Finished knowledge base generation pipeline for pipelineId=[{}]. Results: Success=[{}], NotFound=[{}], Errors=[{}]",
                pipelineId, successCount, notFoundCount, errorCount);
    }
}

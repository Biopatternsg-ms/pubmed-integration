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
import com.biopatternsg.domain.model.PubtatorSearchRequest;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.external_repositories.PubtatorSearchRepoWeb;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorSearchProgressRepository;
import com.biopatternsg.mongo.PubtatorResultCollection;
import com.biopatternsg.mongo.PubtatorSearchProgressCollection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class PubtatorQueueConsumerRabbitImpl {

    private final PubtatorSearchRepoWeb pubtatorSearchRepoWeb;
    private final PubtatorResultRepository pubtatorResultRepository;
    private final ConfigAndControlRepository configAndControlRepository;
    private final PubtatorSearchProgressRepository pubtatorSearchProgressRepository;
    private final ObjectMapper objectMapper;

    @Incoming("pubtator-in")
    @Blocking(ordered = false)
    public void consume(JsonObject jsonMsg) {
        PubtatorSearchRequest request = jsonMsg.mapTo(PubtatorSearchRequest.class);

        try {
//            log.info("[{}/{}] Processing PubTator batch with [{}] PMIDs for pipelineId=[{}]",
//                    request.batchIndex(), request.batchesTotal(),
//                    request.pmids().size(), request.pipelineId());

            List<String> pmids = request.pmids();
            List<String> existingPmids = pubtatorResultRepository.findExistingPmids(pmids);
            List<String> pmidsToFetch = new ArrayList<>(pmids);
            pmidsToFetch.removeAll(existingPmids);

            if (!pmidsToFetch.isEmpty()) {
                log.info("[{}/{}] Fetching {} PMIDs from PubTator API (already cached: {})",
                        request.batchIndex(), request.batchesTotal(), pmidsToFetch.size(), existingPmids.size());
                String responseJson = pubtatorSearchRepoWeb.search(pmidsToFetch);

                if (responseJson != null && !responseJson.isEmpty()) {
                    JsonNode rootNode = objectMapper.readTree(responseJson);
                    JsonNode pubTator3Array = rootNode.get("PubTator3");
                    if (pubTator3Array != null && pubTator3Array.isArray()) {
                        List<PubtatorResultCollection> docsToSave = new ArrayList<>();
                        for (JsonNode docNode : pubTator3Array) {
                            PubtatorResultCollection doc = new PubtatorResultCollection();
                            
                            String pmid = docNode.has("id") ? docNode.get("id").asText() : "";
                            doc.setPmid(pmid);
                            
                            List<PubtatorResultCollection.PubtatorObject> objectsList = new ArrayList<>();
                            List<PubtatorResultCollection.PubtatorEvent> eventsList = new ArrayList<>();
                            
                            // Parse passages (Title and Abstract)
                            JsonNode passages = docNode.get("passages");
                            if (passages != null && passages.isArray()) {
                                for (JsonNode passage : passages) {
                                    JsonNode infons = passage.get("infons");
                                    String type = "";
                                    if (infons != null && infons.has("type")) {
                                        type = infons.get("type").asText();
                                    }
                                    
                                    String text = passage.has("text") ? passage.get("text").asText() : "";
                                    if ("title".equalsIgnoreCase(type)) {
                                        doc.setTitle(text);
                                    } else if ("abstract".equalsIgnoreCase(type)) {
                                        doc.setText(text);
                                    }
                                    
                                    // Parse annotations
                                    JsonNode annotations = passage.get("annotations");
                                    if (annotations != null && annotations.isArray()) {
                                        for (JsonNode ann : annotations) {
                                            JsonNode annInfons = ann.get("infons");
                                            String accession = getInfonValue(annInfons, "accession");
                                            
                                            // Only save objects with accession (not "null" or empty)
                                            if (accession != null && !accession.isEmpty() && !accession.equals("null")) {
                                                PubtatorResultCollection.PubtatorObject obj = new PubtatorResultCollection.PubtatorObject();
                                                obj.setAccession(accession);
                                                obj.setIdentifier(getInfonValue(annInfons, "identifier"));
                                                obj.setName(getInfonValue(annInfons, "name"));
                                                obj.setNormalizedId(getInfonValue(annInfons, "normalized_id"));
                                                obj.setType(getInfonValue(annInfons, "type"));
                                                obj.setBiotype(getInfonValue(annInfons, "biotype"));
                                                obj.setText(ann.has("text") ? ann.get("text").asText() : "");
                                                
                                                // Parse locations
                                                List<PubtatorResultCollection.Location> locList = new ArrayList<>();
                                                JsonNode locations = ann.get("locations");
                                                if (locations != null && locations.isArray()) {
                                                    for (JsonNode loc : locations) {
                                                        PubtatorResultCollection.Location l = new PubtatorResultCollection.Location();
                                                        l.setLength(loc.has("length") ? loc.get("length").asInt() : 0);
                                                        l.setOffset(loc.has("offset") ? loc.get("offset").asInt() : 0);
                                                        locList.add(l);
                                                    }
                                                }
                                                obj.setLocations(locList);
                                                objectsList.add(obj);
                                            }
                                        }
                                    }
                                }
                            }
                            doc.setObjects(objectsList);
                            
                            // Parse relations
                            JsonNode relations = docNode.get("relations");
                            if (relations != null && relations.isArray()) {
                                for (JsonNode rel : relations) {
                                    JsonNode relInfons = rel.get("infons");
                                    if (relInfons != null) {
                                        PubtatorResultCollection.PubtatorEvent event = new PubtatorResultCollection.PubtatorEvent();
                                        
                                        String role1 = "-";
                                        if (relInfons.has("role1") && relInfons.get("role1").has("accession")) {
                                            role1 = relInfons.get("role1").get("accession").asText();
                                        }
                                        
                                        String role2 = "-";
                                        if (relInfons.has("role2") && relInfons.get("role2").has("accession")) {
                                            role2 = relInfons.get("role2").get("accession").asText();
                                        }
                                        
                                        String relationType = relInfons.has("type") ? relInfons.get("type").asText() : "-";
                                        
                                        event.setRole1(role1);
                                        event.setRole2(role2);
                                        event.setRelationType(relationType);
                                        
                                        eventsList.add(event);
                                    }
                                }
                            }
                            doc.setEvents(eventsList);
                            
                            docsToSave.add(doc);
                        }
                        if (!docsToSave.isEmpty()) {
                            pubtatorResultRepository.saveAll(docsToSave);
                        }
                    }
                }
            } else {
                log.info("[{}/{}] All {} PMIDs in batch are already cached in pubtator_results. Skipping API call.",
                        request.batchIndex(), request.batchesTotal(), pmids.size());
            }

            log.info("[{}/{}] PubTator batch processed successfully for pipelineId=[{}]",
                    request.batchIndex(), request.batchesTotal(), request.pipelineId());

        } catch (Exception e) {
            log.error("[{}/{}] Error processing PubTator batch for pipelineId=[{}]: {}",
                    request.batchIndex(), request.batchesTotal(),
                    request.pipelineId(), e.getMessage(), e);
        } finally {
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

    private String getInfonValue(JsonNode infons, String key) {
        if (infons != null && infons.has(key)) {
            return infons.get(key).asText();
        }
        return "-";
    }
}

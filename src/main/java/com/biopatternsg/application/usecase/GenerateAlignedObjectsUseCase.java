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

import com.biopatternsg.domain.model.AlignedAs;
import com.biopatternsg.domain.model.AlignedResult;
import com.biopatternsg.domain.model.ExpertObjectConfig;
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.in.GenerateAlignedObjects;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.repositories.AlignedResultRepository;
import com.biopatternsg.domain.ports.out.repositories.SynonymRepository;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class GenerateAlignedObjectsUseCase implements GenerateAlignedObjects {

    private final SynonymRepository synonymRepository;
    private final AlignedResultRepository alignedResultRepository;
    private final ConfigAndControlRepository configAndControlRepository;

    @Inject
    public GenerateAlignedObjectsUseCase(
            SynonymRepository synonymRepository,
            AlignedResultRepository alignedResultRepository,
            ConfigAndControlRepository configAndControlRepository
    ) {
        this.synonymRepository = synonymRepository;
        this.alignedResultRepository = alignedResultRepository;
        this.configAndControlRepository = configAndControlRepository;
    }

    @Override
    public void execute(String pipelineId, List<ExpertObjectConfig> expertObjects, String userId) {
        log.info("Starting expert objects alignment for pipelineId=[{}] with [{}] expert objects",
                pipelineId, expertObjects != null ? expertObjects.size() : 0);

        try {
            List<ExpertObjectConfig> safeExpertObjects = expertObjects != null ? expertObjects : Collections.emptyList();
            Map<String, List<String>> synonymsMap = fetchSynonyms(pipelineId);

            List<String> userObjects = getNormalizedUserObjects(safeExpertObjects);

            Map<String, List<String>> synonymsMapUpper = getNormalizedSynonyms(synonymsMap);

            List<String> aligned = calculateAlignedObjects(userObjects, synonymsMapUpper);

            Map<String, List<String>> invertedSynonyms = buildInvertedSynonymsIndex(synonymsMapUpper);

            List<AlignedAs> alignedAsList = calculateAlignedAsList(userObjects, invertedSynonyms);

            List<String> noAligned = calculateNoAlignedObjects(userObjects, invertedSynonyms);

            List<String> alignedAndAlternatives = calculateAlignedAndAlternatives(aligned, alignedAsList);

            saveAlignedResult(pipelineId, aligned, noAligned, alignedAsList, alignedAndAlternatives);

            notifyStatus(pipelineId, PipelineSteps.GENERATE_ALIGNED_OBJECTS, Status.COMPLETED, userId);

            log.info("Successfully completed expert objects alignment for pipelineId=[{}]", pipelineId);

        } catch (Exception e) {
            log.error("Fatal error during expert objects alignment for pipelineId=[{}]", pipelineId, e);
            notifyStatus(pipelineId, PipelineSteps.GENERATE_ALIGNED_OBJECTS, Status.FAILED, userId);
            throw new InternalServerError(e);
        }
    }

    private Map<String, List<String>> fetchSynonyms(String pipelineId) {
        Map<String, List<String>> synonymsMap = synonymRepository.findAllByPipelineId(pipelineId);
        log.info("Fetched synonyms dictionary with [{}] entries for pipelineId=[{}]", 
                synonymsMap != null ? synonymsMap.size() : 0, pipelineId);
        return synonymsMap != null ? synonymsMap : Collections.emptyMap();
    }

    private List<String> getNormalizedUserObjects(List<ExpertObjectConfig> expertObjects) {
        return expertObjects.stream()
                .map(ExpertObjectConfig::symbol)
                .filter(symbol -> symbol != null && !symbol.trim().isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }

    private Map<String, List<String>> getNormalizedSynonyms(Map<String, List<String>> synonymsMap) {
        return synonymsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toUpperCase(),
                        entry -> entry.getValue().stream()
                                .filter(syn -> syn != null && !syn.trim().isEmpty())
                                .map(String::toUpperCase)
                                .distinct()
                                .toList(),
                        (v1, v2) -> {
                            List<String> merged = new ArrayList<>(v1);
                            merged.addAll(v2);
                            return merged.stream().distinct().toList();
                        }
                ));
    }

    private List<String> calculateAlignedObjects(List<String> userObjects, Map<String, List<String>> synonymsMapUpper) {
        return userObjects.stream()
                .filter(synonymsMapUpper::containsKey)
                .toList();
    }

    private Map<String, List<String>> buildInvertedSynonymsIndex(Map<String, List<String>> synonymsMapUpper) {
        Map<String, List<String>> inverted = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : synonymsMapUpper.entrySet()) {
            String mainId = entry.getKey();
            for (String synonym : entry.getValue()) {
                inverted.computeIfAbsent(synonym, k -> new ArrayList<>()).add(mainId);
            }
        }
        return inverted;
    }

    private List<AlignedAs> calculateAlignedAsList(List<String> userObjects, Map<String, List<String>> invertedSynonyms) {
        List<AlignedAs> alignedAsList = new ArrayList<>();
        for (String name : userObjects) {
            List<String> altIds = invertedSynonyms.getOrDefault(name, Collections.emptyList());
            if (!altIds.isEmpty()) {
                if (altIds.size() > 1 || (altIds.size() == 1 && !altIds.get(0).equals(name))) {
                    alignedAsList.add(new AlignedAs(name, altIds));
                }
            }
        }
        return alignedAsList;
    }

    private List<String> calculateNoAlignedObjects(List<String> userObjects, Map<String, List<String>> invertedSynonyms) {
        return userObjects.stream()
                .filter(name -> !invertedSynonyms.containsKey(name) || invertedSynonyms.get(name).isEmpty())
                .toList();
    }

    private List<String> calculateAlignedAndAlternatives(List<String> aligned, List<AlignedAs> alignedAsList) {
        Set<String> relatedObjects = new HashSet<>(aligned);
        for (AlignedAs item : alignedAsList) {
            relatedObjects.addAll(item.alternativeIds());
        }
        return new ArrayList<>(relatedObjects);
    }

    private void saveAlignedResult(
            String pipelineId,
            List<String> aligned,
            List<String> noAligned,
            List<AlignedAs> alignedAsList,
            List<String> alignedAndAlternatives
    ) {
        AlignedResult alignedResult = new AlignedResult(
                pipelineId,
                aligned,
                noAligned,
                alignedAsList,
                alignedAndAlternatives
        );
        alignedResultRepository.save(alignedResult);
    }

    private void notifyStatus(String pipelineId, PipelineSteps step, Status status, String userId) {
        configAndControlRepository.updateStep(pipelineId, step, status, userId);
    }
}

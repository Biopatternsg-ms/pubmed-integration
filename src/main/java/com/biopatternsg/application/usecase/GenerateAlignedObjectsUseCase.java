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
import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.domain.ports.in.GenerateAlignedObjects;
import com.biopatternsg.domain.ports.out.external_repositories.BiologicalObjectsRepository;
import com.biopatternsg.domain.ports.out.repositories.AlignedResultRepository;
import com.biopatternsg.domain.ports.out.repositories.SynonymRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class GenerateAlignedObjectsUseCase implements GenerateAlignedObjects {

    private final BiologicalObjectsRepository biologicalObjectsRepository;
    private final SynonymRepository synonymRepository;
    private final AlignedResultRepository alignedResultRepository;

    @Inject
    public GenerateAlignedObjectsUseCase(
            BiologicalObjectsRepository biologicalObjectsRepository,
            SynonymRepository synonymRepository,
            AlignedResultRepository alignedResultRepository
    ) {
        this.biologicalObjectsRepository = biologicalObjectsRepository;
        this.synonymRepository = synonymRepository;
        this.alignedResultRepository = alignedResultRepository;
    }

    @Override
    public void execute(String pipelineId) {
        log.info("Starting expert objects alignment for pipelineId=[{}]", pipelineId);

        try {
            // 1. Fetch expert biological objects for level 2
            List<BiologicalObject> expertObjects = biologicalObjectsRepository.expertObjectsByPipelineAndLevel(pipelineId, 2);
            log.info("Fetched [{}] expert biological objects for pipelineId=[{}]", expertObjects.size(), pipelineId);

            // 2. Fetch all synonyms for the pipeline
            Map<String, List<String>> synonymsMap = synonymRepository.findAllByPipelineId(pipelineId);
            log.info("Fetched synonyms dictionary with [{}] entries for pipelineId=[{}]", synonymsMap.size(), pipelineId);

            // 3. Convert user objects names to uppercase and distinct
            List<String> userObjects = expertObjects.stream()
                    .map(BiologicalObject::name)
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .map(String::toUpperCase)
                    .distinct()
                    .collect(Collectors.toList());

            // Normalize synonyms map to uppercase keys and uppercase synonym lists for robust matching
            Map<String, List<String>> synonymsMapUpper = synonymsMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().toUpperCase(),
                            entry -> entry.getValue().stream()
                                    .filter(syn -> syn != null && !syn.trim().isEmpty())
                                    .map(String::toUpperCase)
                                    .collect(Collectors.toList()),
                            (v1, v2) -> {
                                List<String> merged = new ArrayList<>(v1);
                                merged.addAll(v2);
                                return merged;
                            }
                    ));

            // 4. Calculate aligned objects (exist as key in synonyms)
            List<String> aligned = userObjects.stream()
                    .filter(synonymsMapUpper::containsKey)
                    .collect(Collectors.toList());

            // 5. Calculate alternatives aligned_as mapping
            Map<String, List<String>> alignedAsMap = new LinkedHashMap<>();
            for (String name : userObjects) {
                List<String> altIds = new ArrayList<>();
                for (Map.Entry<String, List<String>> entry : synonymsMapUpper.entrySet()) {
                    String mainId = entry.getKey();
                    List<String> synonymsList = entry.getValue();
                    if (synonymsList.contains(name)) {
                        if (!altIds.contains(mainId)) {
                            altIds.add(mainId);
                        }
                    }
                }
                alignedAsMap.put(name, altIds);
            }

            // 6. Calculate no_aligned objects (altIds list is empty)
            List<String> noAligned = alignedAsMap.entrySet().stream()
                    .filter(entry -> entry.getValue().isEmpty())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 7. Filter aligned_as entries
            List<AlignedAs> alignedAsList = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : alignedAsMap.entrySet()) {
                String expertName = entry.getKey();
                List<String> altIds = entry.getValue();
                if (altIds.size() == 1 && !altIds.get(0).equals(expertName)) {
                    alignedAsList.add(new AlignedAs(expertName, altIds));
                } else if (altIds.size() > 1) {
                    alignedAsList.add(new AlignedAs(expertName, altIds));
                }
            }

            // 8. Calculate aligned_and_alternatives (union of aligned + all alternative IDs)
            Set<String> alternativeIdsSet = new HashSet<>();
            for (AlignedAs item : alignedAsList) {
                alternativeIdsSet.addAll(item.alternativeIds());
            }

            Set<String> relatedObjects = new HashSet<>(aligned);
            relatedObjects.addAll(alternativeIdsSet);
            List<String> alignedAndAlternatives = new ArrayList<>(relatedObjects);

            // 9. Save AlignedResult to MongoDB
            AlignedResult alignedResult = new AlignedResult(
                    pipelineId,
                    aligned,
                    noAligned,
                    alignedAsList,
                    alignedAndAlternatives
            );

            alignedResultRepository.save(alignedResult);
            log.info("Successfully completed expert objects alignment for pipelineId=[{}]", pipelineId);

        } catch (Exception e) {
            log.error("Fatal error during expert objects alignment for pipelineId=[{}]", pipelineId, e);
            throw e;
        }
    }
}

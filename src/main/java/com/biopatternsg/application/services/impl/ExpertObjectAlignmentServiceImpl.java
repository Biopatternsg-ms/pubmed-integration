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
package com.biopatternsg.application.services.impl;

import com.biopatternsg.application.services.ExpertObjectAlignmentService;
import com.biopatternsg.domain.model.AlignedAs;
import com.biopatternsg.domain.model.AlignedResult;
import com.biopatternsg.domain.model.ExpertObjectConfig;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ExpertObjectAlignmentServiceImpl implements ExpertObjectAlignmentService {

    @Override
    public AlignedResult align(
            String pipelineId, List<ExpertObjectConfig> expertObjects, Map<String, List<String>> synonymsMap) {
        List<ExpertObjectConfig> safeExpertObjects = expertObjects != null ? expertObjects : Collections.emptyList();
        Map<String, List<String>> safeSynonymsMap = synonymsMap != null ? synonymsMap : Collections.emptyMap();

        List<String> userObjects = normalizeUserObjects(safeExpertObjects);
        Map<String, List<String>> synonymsMapUpper = normalizeSynonyms(safeSynonymsMap);

        List<String> aligned = calculateAlignedObjects(userObjects, synonymsMapUpper);
        Map<String, List<String>> invertedSynonyms = buildInvertedSynonymsIndex(synonymsMapUpper);
        List<AlignedAs> alignedAsList = calculateAlignedAsList(userObjects, invertedSynonyms);
        List<String> noAligned = calculateNoAlignedObjects(userObjects, invertedSynonyms);
        List<String> alignedAndAlternatives = calculateAlignedAndAlternatives(aligned, alignedAsList);

        return new AlignedResult(pipelineId, aligned, noAligned, alignedAsList, alignedAndAlternatives);
    }

    private List<String> normalizeUserObjects(List<ExpertObjectConfig> expertObjects) {
        return expertObjects.stream()
                .map(ExpertObjectConfig::symbol)
                .filter(symbol -> symbol != null && !symbol.trim().isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }

    private Map<String, List<String>> normalizeSynonyms(Map<String, List<String>> synonymsMap) {
        return synonymsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toUpperCase(),
                        entry -> normalizeSynonymList(entry.getValue()),
                        this::mergeDistinct
                ));
    }

    private List<String> normalizeSynonymList(List<String> synonyms) {
        if (synonyms == null) {
            return List.of();
        }
        return synonyms.stream()
                .filter(syn -> syn != null && !syn.trim().isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }

    private List<String> mergeDistinct(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<>(first);
        merged.addAll(second);
        return merged.stream().distinct().toList();
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

    private List<AlignedAs> calculateAlignedAsList(
            List<String> userObjects, Map<String, List<String>> invertedSynonyms) {
        List<AlignedAs> alignedAsList = new ArrayList<>();
        for (String name : userObjects) {
            List<String> altIds = invertedSynonyms.getOrDefault(name, Collections.emptyList());
            if (!altIds.isEmpty() && (altIds.size() > 1 || !altIds.get(0).equals(name))) {
                alignedAsList.add(new AlignedAs(name, altIds));
            }
        }
        return alignedAsList;
    }

    private List<String> calculateNoAlignedObjects(
            List<String> userObjects, Map<String, List<String>> invertedSynonyms) {
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
}

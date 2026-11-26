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

import com.biopatternsg.application.services.BiologicalObjectsService;
import com.biopatternsg.application.services.PubmedPairsBuilderService;
import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.domain.model.Pair;
import com.biopatternsg.domain.model.TermPair;
import com.biopatternsg.domain.util.LogSanitizer;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@ApplicationScoped
public class PubmedPairsBuilderServiceImpl implements PubmedPairsBuilderService {

    private static final int FIRST_LEVEL = 1;

    private final BiologicalObjectsService biologicalObjectsService;
    private final int maxSynonyms;

    public PubmedPairsBuilderServiceImpl(
            BiologicalObjectsService biologicalObjectsService,
            @ConfigProperty(name = "pubmed.max-synonyms-per-object", defaultValue = "10") int maxSynonyms) {
        this.biologicalObjectsService = biologicalObjectsService;
        this.maxSynonyms = maxSynonyms;
    }

    @Override
    public Set<TermPair> buildFirstLevelPairs(String pipelineId, boolean useOnlyPrincipalName) {
        List<BiologicalObject> biologicalObjects = biologicalObjectsService.getBiologicalObjectsByLevel(pipelineId, FIRST_LEVEL);
        log.info("Biological objects level [{}]: {}", FIRST_LEVEL, biologicalObjects.size());

        Map<String, BiologicalObject> biologicalObjectMap = biologicalObjects.stream()
                .collect(Collectors.toMap(BiologicalObject::id, Function.identity()));

        Set<Pair<String, String>> biologicalObjectIdPairs = getBiologicalObjectPairs(
                biologicalObjectMap.keySet().stream().toList());

        Set<TermPair> uniquePairs = biologicalObjectIdPairs.stream()
                .flatMap(pair -> buildTermsBetweenObjects(
                        useOnlyPrincipalName,
                        biologicalObjectMap.get(pair.first()),
                        biologicalObjectMap.get(pair.second())).stream())
                .collect(Collectors.toSet());

        log.info("Generated unique pairs for first level: {}", uniquePairs.size());
        return uniquePairs;
    }

    @Override
    public List<Set<TermPair>> buildHigherLevelPairBatches(String pipelineId, boolean useOnlyPrincipalName, int level) {
        List<BiologicalObject> currentBiologicalObjects = biologicalObjectsService.getBiologicalObjectsByLevel(pipelineId, level);
        log.info("Processing level [{}]: {} objects", level, currentBiologicalObjects.size());

        return currentBiologicalObjects.parallelStream()
                .map(currentObj -> buildFamilyPairsForObject(pipelineId, useOnlyPrincipalName, level, currentObj))
                .toList();
    }

    private Set<TermPair> buildFamilyPairsForObject(
            String pipelineId, boolean useOnlyPrincipalName, int level, BiologicalObject currentBiologicalObject) {
        List<BiologicalObject> biologicalObjectFamily =
                biologicalObjectsService.getBiologicalObjectFatherBrothersAndSons(pipelineId, currentBiologicalObject.id());

        Set<TermPair> uniquePairs = biologicalObjectFamily.parallelStream()
                .filter(biologicalObject -> !biologicalObject.id().equals(currentBiologicalObject.id()))
                .flatMap(biologicalObject -> buildTermsBetweenObjects(
                        useOnlyPrincipalName, currentBiologicalObject, biologicalObject).stream())
                .collect(Collectors.toSet());

        log.info("---- LEVEL [{}] biologicalObject [{}]: {} pairs", level, LogSanitizer.sanitize(currentBiologicalObject.id()), uniquePairs.size());
        return uniquePairs;
    }

    private Set<TermPair> buildTermsBetweenObjects(
            boolean useOnlyPrincipalName, BiologicalObject biologicalObjectA, BiologicalObject biologicalObjectB) {
        List<String> objectATerms = getTerms(biologicalObjectA, useOnlyPrincipalName);
        List<String> objectBTerms = getTerms(biologicalObjectB, useOnlyPrincipalName);

        Set<TermPair> result = new HashSet<>();
        for (String termA : objectATerms) {
            for (String termB : objectBTerms) {
                if (!termA.equalsIgnoreCase(termB)) {
                    result.add(orderedTermPair(termA, termB));
                }
            }
        }
        return result;
    }

    private TermPair orderedTermPair(String first, String second) {
        return first.compareTo(second) <= 0
                ? new TermPair(first, second)
                : new TermPair(second, first);
    }

    private List<String> getTerms(BiologicalObject obj, boolean useOnlyPrincipalName) {
        return useOnlyPrincipalName
                ? Stream.of(obj.name(), obj.symbol()).filter(Objects::nonNull).toList()
                : obj.allTerms(maxSynonyms);
    }

    private Set<Pair<String, String>> getBiologicalObjectPairs(List<String> biologicalObjectIds) {
        Set<Pair<String, String>> biologicalObjectIdPairs = new HashSet<>();
        for (int i = 0; i < biologicalObjectIds.size(); i++) {
            for (int j = i + 1; j < biologicalObjectIds.size(); j++) {
                biologicalObjectIdPairs.add(getOrderedPair(biologicalObjectIds.get(i), biologicalObjectIds.get(j)));
            }
        }
        return biologicalObjectIdPairs;
    }

    private Pair<String, String> getOrderedPair(String firstTerm, String secondTerm) {
        return firstTerm.compareTo(secondTerm) <= 0
                ? new Pair<>(firstTerm, secondTerm)
                : new Pair<>(secondTerm, firstTerm);
    }
}

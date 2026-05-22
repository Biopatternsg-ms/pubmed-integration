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

import com.biopatternsg.application.services.BiologicalObjectsService;
import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.domain.model.Pair;
import com.biopatternsg.domain.ports.in.BuildPubmedPairs;
import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import com.biopatternsg.mongo.PairsCollection;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class BuildPubmedPairsUseCase implements BuildPubmedPairs {

    private final BiologicalObjectsService biologicalObjectsService;
    private final PairRepository pairRepository;
    private static final int FIRST_LEVEL = 1;
    private static final int SECOND_LEVEL = 2;

    @Override
    public void execute(String pipelineId, boolean useOnlyPrincipalName, int levels) {

        pairRepository.deleteByPipelineId(pipelineId);

        Set<PairsGenerator.TermsPair> firstLevelPairs = processFirstLevel(pipelineId, useOnlyPrincipalName);
        savePairs(firstLevelPairs, pipelineId);

        for (int currentLevel = SECOND_LEVEL; currentLevel <= levels; currentLevel++) {
            processHigherLevel(pipelineId, useOnlyPrincipalName, currentLevel);
        }

        log.info("Generated unique pairs FINISHED");
    }

    private void processHigherLevel(String pipelineId, boolean useOnlyPrincipalName, int level) {
        List<BiologicalObject> currentBiologicalObjects = biologicalObjectsService.getBiologicalObjectsByLevel(pipelineId, level);
        log.info("-----------------------------------------");
        log.info("Processing level [{}]: {} objects", level, currentBiologicalObjects.size());

        currentBiologicalObjects.parallelStream().forEach(currentBiologicalObject -> {

            List<BiologicalObject> biologicalObjectFamily = biologicalObjectsService.getBiologicalObjectFatherBrothersAndSons(pipelineId, currentBiologicalObject.id());

            Set<PairsGenerator.TermsPair> uniquePairs = new HashSet<>();

            biologicalObjectFamily.parallelStream().forEach(biologicalObject -> {
                if (!biologicalObject.id().equals(currentBiologicalObject.id())) {
                    uniquePairs.addAll(buildTermsBetweenObjects(useOnlyPrincipalName, currentBiologicalObject, biologicalObject));
                }
            });
            
            savePairs(uniquePairs, pipelineId);
            log.info("---- LEVEL [{}] biologicalObject [{}]: {} pairs", level, currentBiologicalObject.id(), uniquePairs.size());
        });
    }

    private void savePairs(Set<PairsGenerator.TermsPair> pairs, String pipelineId) {

        PairsCollection toSave = new PairsCollection();
        toSave.setPipelineId(pipelineId);
        toSave.setPairs(
                pairs.stream()
                .map( p -> {

                    PairsCollection.Pair pair = new PairsCollection.Pair();
                    pair.setFirstTerm(p.first);
                    pair.setSecondTerm(p.second);

                    return pair;
                }).collect(Collectors.toList()));

        pairRepository.save(toSave);
    }

    private Set<PairsGenerator.TermsPair> processFirstLevel(String pipelineId, boolean useOnlyPrincipalName) {
        List<BiologicalObject> biologicalObjects = biologicalObjectsService.getBiologicalObjectsByLevel(pipelineId, FIRST_LEVEL);
        log.info("Biological objects level [{}]: {}", FIRST_LEVEL, biologicalObjects.size());

        Map<String, BiologicalObject> biologicalObjectMap = biologicalObjects.stream()
                .collect(Collectors.toMap(BiologicalObject::id, Function.identity()));

        Set<Pair<String, String>> biologicalObjectIdPairs = getBiologicalObjectPairs(biologicalObjectMap.keySet().stream().toList());

        Set<PairsGenerator.TermsPair> uniquePairs = new HashSet<>();

        biologicalObjectIdPairs.forEach(pair -> {
            uniquePairs.addAll(buildTermsBetweenObjects(useOnlyPrincipalName, biologicalObjectMap.get(pair.first()), biologicalObjectMap.get(pair.second())));
        });

        log.info("Generated unique pairs for first level: {}", uniquePairs.size());

        return uniquePairs;
    }

    private Set<PairsGenerator.TermsPair> buildTermsBetweenObjects(boolean useOnlyPrincipalName, BiologicalObject biologicalObjectA, BiologicalObject biologicalObjectB) {

        List<String> objectATerms = getTerms(biologicalObjectA, useOnlyPrincipalName);
        List<String> objectBTerms = getTerms(biologicalObjectB, useOnlyPrincipalName);

        Set<PairsGenerator.TermsPair> resultado = new HashSet<>();

        for (String termA : objectATerms) {
            for (String termB : objectBTerms) {
                if (!termA.equalsIgnoreCase(termB)) {
                    resultado.add(new PairsGenerator.TermsPair(termA, termB));
                }
            }
        }

        return resultado;
    }
    
    private List<String> getTerms(BiologicalObject biologicalObject, boolean useOnlyPrincipalName) {

        List<String> terms = new ArrayList<>();
        Optional.ofNullable(biologicalObject.name()).ifPresent(terms::add);
        Optional.ofNullable(biologicalObject.symbol()).ifPresent(terms::add);

        if (!useOnlyPrincipalName && biologicalObject.synonyms() != null) {
            biologicalObject.synonyms().stream().filter(Objects::nonNull).forEach(terms::add);
        }

        return terms;
    }

    private Set<Pair<String, String>> getBiologicalObjectPairs(List<String> biologicalObjectIds) {

        Set<Pair<String, String>> biologicalObjectIdPairs = new HashSet<>();

        for (int i = 0; i < biologicalObjectIds.size(); i++) {
            for (int j = i + 1;  j < biologicalObjectIds.size(); j++) {
                biologicalObjectIdPairs.add(getOrderedPair(biologicalObjectIds.get(i), biologicalObjectIds.get(j)));
            }
        }

        return biologicalObjectIdPairs;
    }

    private Pair<String, String> getOrderedPair(String firstTerm, String secondTerm) {
        if (firstTerm.compareTo(secondTerm) <= 0) {
            return new Pair<>(firstTerm, secondTerm);
        } else {
            return new Pair<>(secondTerm, firstTerm);
        }
    }
}

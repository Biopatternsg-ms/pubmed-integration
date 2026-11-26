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
import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.domain.model.TermPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PubmedPairsBuilderServiceImplTest {

    private static final String PIPELINE_ID = "pipeline-001";

    @Mock
    private BiologicalObjectsService biologicalObjectsService;

    private PubmedPairsBuilderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PubmedPairsBuilderServiceImpl(biologicalObjectsService, 10);
    }

    private String key(TermPair pair) {
        return pair.firstTerm().compareTo(pair.secondTerm()) <= 0
                ? pair.firstTerm() + "|" + pair.secondTerm()
                : pair.secondTerm() + "|" + pair.firstTerm();
    }

    @Test
    void buildFirstLevelPairs_threeObjectsWithName_generatesThreePairs() {
        BiologicalObject objA = new BiologicalObject("id-A", "GenA", null, null);
        BiologicalObject objB = new BiologicalObject("id-B", "GenB", null, null);
        BiologicalObject objC = new BiologicalObject("id-C", "GenC", null, null);

        when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                .thenReturn(List.of(objA, objB, objC));

        Set<TermPair> pairs = service.buildFirstLevelPairs(PIPELINE_ID, true);

        assertThat(pairs.stream().map(this::key).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("GenA|GenB", "GenA|GenC", "GenB|GenC");
    }

    @Test
    void buildFirstLevelPairs_respectsMaxSynonyms() {
        PubmedPairsBuilderServiceImpl limitedService = new PubmedPairsBuilderServiceImpl(biologicalObjectsService, 1);
        BiologicalObject objA = new BiologicalObject("id-A", "BRCA1", "BR1", List.of("syn1", "syn2", "syn3"));
        BiologicalObject objB = new BiologicalObject("id-B", "TP53", null, null);

        when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                .thenReturn(List.of(objA, objB));

        Set<TermPair> pairs = limitedService.buildFirstLevelPairs(PIPELINE_ID, false);

        assertThat(pairs.stream().map(this::key).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("BRCA1|TP53", "BR1|TP53", "TP53|syn1");
    }

    @Test
    void buildHigherLevelPairBatches_largeFamilyConcurrentExecution_isDeterministicAndThreadSafe() {
        BiologicalObject root = new BiologicalObject("id-0", "ROOT", null, null);
        List<BiologicalObject> family = new ArrayList<>();
        family.add(root);
        for (int i = 1; i <= 50; i++) {
            family.add(new BiologicalObject("id-" + i, "GENE_" + i, null, null));
        }

        when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 2))
                .thenReturn(List.of(root));
        when(biologicalObjectsService.getBiologicalObjectFatherBrothersAndSons(PIPELINE_ID, "id-0"))
                .thenReturn(family);

        List<Set<TermPair>> batches = service.buildHigherLevelPairBatches(PIPELINE_ID, true, 2);

        assertThat(batches).hasSize(1);
        assertThat(batches.get(0)).hasSize(50);
    }
}

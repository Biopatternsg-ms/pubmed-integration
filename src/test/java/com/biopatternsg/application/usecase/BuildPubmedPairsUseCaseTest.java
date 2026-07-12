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
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import com.biopatternsg.mongo.PairsCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuildPubmedPairsUseCase - Pair construction logic")
class BuildPubmedPairsUseCaseTest {

    private static final String PIPELINE_ID = "pipeline-001";
    private static final String USER_ID     = "user-001";

    @Mock private BiologicalObjectsService biologicalObjectsService;
    @Mock private PairRepository pairRepository;
    @Mock private ConfigAndControlRepository configAndControlRepository;

    private BuildPubmedPairsUseCase useCase;

    private Set<String> captureAllSavedPairs() {
        ArgumentCaptor<PairsCollection> captor = ArgumentCaptor.forClass(PairsCollection.class);
        verify(pairRepository, atLeastOnce()).save(captor.capture());

        return captor.getAllValues().stream()
                .flatMap(pc -> pc.getPairs().stream())
                .map(p -> normalized(p.getFirstTerm(), p.getSecondTerm()))
                .collect(Collectors.toSet());
    }

    private String normalized(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    private String pair(String a, String b) {
        return normalized(a, b);
    }

    @BeforeEach
    void setUp() {
        useCase = new BuildPubmedPairsUseCase(biologicalObjectsService, pairRepository, configAndControlRepository);
        doNothing().when(pairRepository).deleteByPipelineId(anyString());
        doNothing().when(configAndControlRepository).updateStep(anyString(), any(), any(), anyString());
    }

    // =========================================================================
    // LEVEL 1 — Root objects combined with each other
    // =========================================================================

    @Nested
    @DisplayName("Level 1")
    class LevelOne {

        @Test
        @DisplayName("Two objects with name only → generates exactly one pair of terms")
        void twoObjectsWithName_generatesOnePair() {
            BiologicalObject objA = new BiologicalObject("id-A", "BRCA1", null, null);
            BiologicalObject objB = new BiologicalObject("id-B", "TP53",  null, null);

            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of(objA, objB));

            useCase.execute(PIPELINE_ID, true, 1, USER_ID);

            Set<String> savedPairs = captureAllSavedPairs();
            assertThat(savedPairs)
                    .hasSize(1)
                    .contains(pair("BRCA1", "TP53"));
        }

        @Test
        @DisplayName("Three objects with name → generates exactly 3 pairs (combinatorics C(3,2))")
        void threeObjectsWithName_generatesThreePairs() {
            BiologicalObject objA = new BiologicalObject("id-A", "GenA", null, null);
            BiologicalObject objB = new BiologicalObject("id-B", "GenB", null, null);
            BiologicalObject objC = new BiologicalObject("id-C", "GenC", null, null);

            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of(objA, objB, objC));

            useCase.execute(PIPELINE_ID, true, 1, USER_ID);

            Set<String> savedPairs = captureAllSavedPairs();
            assertThat(savedPairs)
                    .hasSize(3)
                    .contains(pair("GenA", "GenB"), pair("GenA", "GenC"), pair("GenB", "GenC"));
        }

        @Test
        @DisplayName("Single object → generates no pairs")
        void singleObject_noPairs() {
            BiologicalObject solo = new BiologicalObject("id-A", "OnlyGene", null, null);

            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of(solo));

            useCase.execute(PIPELINE_ID, true, 1, USER_ID);

            ArgumentCaptor<PairsCollection> captor = ArgumentCaptor.forClass(PairsCollection.class);
            verify(pairRepository, atLeastOnce()).save(captor.capture());

            long totalPairs = captor.getAllValues().stream()
                    .mapToLong(pc -> pc.getPairs().size())
                    .sum();
            assertThat(totalPairs).isZero();
        }

        @Test
        @DisplayName("No objects → generates no pairs")
        void noObjects_noPairs() {
            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of());

            useCase.execute(PIPELINE_ID, true, 1, USER_ID);

            ArgumentCaptor<PairsCollection> captor = ArgumentCaptor.forClass(PairsCollection.class);
            verify(pairRepository, atLeastOnce()).save(captor.capture());

            long totalPairs = captor.getAllValues().stream()
                    .mapToLong(pc -> pc.getPairs().size())
                    .sum();
            assertThat(totalPairs).isZero();
        }
    }

    // =========================================================================
    // SYNONYMS — useOnlyPrincipalName = false
    // =========================================================================

    @Nested
    @DisplayName("Synonyms (useOnlyPrincipalName = false)")
    class Synonyms {

        @Test
        @DisplayName("Object A with synonyms × Object B with name → generates pairs for each synonym")
        void synonymsCrossWithNameOfOtherObject() {
            BiologicalObject objA = new BiologicalObject("id-A", "BRCA1", "BR1", List.of("breast cancer 1"));
            BiologicalObject objB = new BiologicalObject("id-B", "TP53",  null, null);

            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of(objA, objB));

            useCase.execute(PIPELINE_ID, false, 1, USER_ID);

            Set<String> savedPairs = captureAllSavedPairs();
            assertThat(savedPairs).contains(
                    pair("BRCA1", "TP53"),
                    pair("BR1",   "TP53"),
                    pair("breast cancer 1", "TP53")
            );
        }

        @Test
        @DisplayName("useOnlyPrincipalName=true ignores synonyms and symbol (name only)")
        void useOnlyPrincipalNameIgnoresSynonyms() {
            BiologicalObject objA = new BiologicalObject("id-A", "BRCA1", "BR1", List.of("breast cancer 1"));
            BiologicalObject objB = new BiologicalObject("id-B", "TP53",  null, null);

            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of(objA, objB));

            useCase.execute(PIPELINE_ID, true, 1, USER_ID);

            Set<String> savedPairs = captureAllSavedPairs();
            assertThat(savedPairs)
                    .contains(pair("BRCA1", "TP53"), pair("BR1", "TP53"))
                    .doesNotContain(pair("breast cancer 1", "TP53"));
        }

        @Test
        @DisplayName("Identical terms between objects do not generate a pair (case-insensitive)")
        void identicalTermsDoNotCrossPair() {
            BiologicalObject objA = new BiologicalObject("id-A", "GENE1", null, null);
            BiologicalObject objB = new BiologicalObject("id-B", "gene1", null, null);

            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of(objA, objB));

            useCase.execute(PIPELINE_ID, true, 1, USER_ID);

            ArgumentCaptor<PairsCollection> captor = ArgumentCaptor.forClass(PairsCollection.class);
            verify(pairRepository, atLeastOnce()).save(captor.capture());

            long totalPairs = captor.getAllValues().stream()
                    .mapToLong(pc -> pc.getPairs().size())
                    .sum();
            assertThat(totalPairs).isZero();
        }

        @Test
        @DisplayName("Pairs are unique even if the same term appears in synonyms of both objects")
        void globalDeduplicationOfPairs() {
            BiologicalObject objA = new BiologicalObject("id-A", "GeneA", null, List.of("shared"));
            BiologicalObject objB = new BiologicalObject("id-B", "shared", null, null);

            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of(objA, objB));

            useCase.execute(PIPELINE_ID, false, 1, USER_ID);

            Set<String> savedPairs = captureAllSavedPairs();
            assertThat(savedPairs).contains(pair("GeneA", "shared"));
            assertThat(savedPairs).doesNotContain(pair("shared", "shared"));
        }

        @Test
        @DisplayName("Duplicated name and symbol in synonyms → pair is generated only once")
        void duplicatedNameAndSymbolInSynonyms_doesNotDuplicatePairs() {
            BiologicalObject objA = new BiologicalObject("id-A", "BRCA1", "BR1",
                    List.of("BRCA1", "BR1", "breast cancer 1"));
            BiologicalObject objB = new BiologicalObject("id-B", "TP53", null, null);

            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of(objA, objB));

            useCase.execute(PIPELINE_ID, false, 1, USER_ID);

            Set<String> savedPairs = captureAllSavedPairs();

            assertThat(savedPairs)
                    .hasSize(3)
                    .containsExactlyInAnyOrder(
                            pair("BRCA1", "TP53"),
                            pair("BR1",   "TP53"),
                            pair("breast cancer 1", "TP53")
                    );
        }
    }

    // =========================================================================
    // HIGHER LEVELS — Family processing (father, siblings, children)
    // =========================================================================

    @Nested
    @DisplayName("Higher levels (level >= 2)")
    class HigherLevels {

        @Test
        @DisplayName("Level 2: each object combines with its family, not with itself")
        void level2_eachObjectWithFamily() {
            BiologicalObject child = new BiologicalObject("id-child", "ChildGene", null, null);
            BiologicalObject father = new BiologicalObject("id-father", "FatherGene", null, null);
            BiologicalObject brother = new BiologicalObject("id-brother", "BrotherGene", null, null);

            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of());
            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 2))
                    .thenReturn(List.of(child));
            when(biologicalObjectsService.getBiologicalObjectFatherBrothersAndSons(PIPELINE_ID, "id-child"))
                    .thenReturn(List.of(child, father, brother));

            useCase.execute(PIPELINE_ID, true, 2, USER_ID);

            Set<String> savedPairs = captureAllSavedPairs();
            assertThat(savedPairs)
                    .contains(pair("ChildGene", "FatherGene"), pair("ChildGene", "BrotherGene"))
                    .doesNotContain(pair("ChildGene", "ChildGene"));
        }

        @Test
        @DisplayName("Level 2: if family only contains the object itself, no pairs are generated")
        void level2_familyWithOnlyItself_noPairs() {
            BiologicalObject loner = new BiologicalObject("id-loner", "LonerGene", null, null);

            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of());
            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 2))
                    .thenReturn(List.of(loner));
            when(biologicalObjectsService.getBiologicalObjectFatherBrothersAndSons(PIPELINE_ID, "id-loner"))
                    .thenReturn(List.of(loner));

            useCase.execute(PIPELINE_ID, true, 2, USER_ID);

            ArgumentCaptor<PairsCollection> captor = ArgumentCaptor.forClass(PairsCollection.class);
            verify(pairRepository, atLeastOnce()).save(captor.capture());

            long totalPairs = captor.getAllValues().stream()
                    .mapToLong(pc -> pc.getPairs().size())
                    .sum();
            assertThat(totalPairs).isZero();
        }

        @Test
        @DisplayName("levels=1 must not invoke getBiologicalObjectsByLevel for level 2")
        void levels1_doesNotInvokeLevel2() {
            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of());

            useCase.execute(PIPELINE_ID, true, 1, USER_ID);

            verify(biologicalObjectsService, never()).getBiologicalObjectsByLevel(PIPELINE_ID, 2);
        }
    }

    // =========================================================================
    // INFRASTRUCTURE — Expected behavior regardless of pairs
    // =========================================================================

    @Nested
    @DisplayName("Infrastructure interaction")
    class Infrastructure {

        @Test
        @DisplayName("Always deletes pipeline pairs before generating new ones")
        void alwaysDeletesBeforeProcessing() {
            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of());

            useCase.execute(PIPELINE_ID, true, 1, USER_ID);

            verify(pairRepository).deleteByPipelineId(PIPELINE_ID);
        }

        @Test
        @DisplayName("Always updates COMBINATIONS step to COMPLETED at the end")
        void alwaysUpdatesStatusAtTheEnd() {
            when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                    .thenReturn(List.of());

            useCase.execute(PIPELINE_ID, true, 1, USER_ID);

            verify(configAndControlRepository).updateStep(
                    eq(PIPELINE_ID),
                    eq(com.biopatternsg.domain.model.PipelineSteps.COMBINATIONS),
                    eq(com.biopatternsg.domain.model.Status.COMPLETED),
                    eq(USER_ID)
            );
        }
    }
}

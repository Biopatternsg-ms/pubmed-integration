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
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.repositories.KbEventRepository;
import com.biopatternsg.domain.ports.out.repositories.KbObjectRepository;
import com.biopatternsg.domain.ports.out.repositories.PubmedResultRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorPmidReaderRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import com.biopatternsg.domain.ports.out.external_repositories.BuildKnowledgeBaseRepoWeb;
import com.biopatternsg.domain.ports.out.external_repositories.GenerateKbResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateKbForPipelineUseCase - Knowledge base events and objects generation process")
class GenerateKbForPipelineUseCaseTest {

    private static final String PIPELINE_ID = "pipeline-123";
    private static final String USER_ID = "user-456";
    private static final String PMID = "12345678";

    @Mock private PubtatorPmidReaderRepository pubtatorPmidReaderRepository;
    @Mock private PubtatorResultRepository pubtatorResultRepository;
    @Mock private BuildKnowledgeBaseRepoWeb buildKnowledgeBaseRepoWeb;
    @Mock private KbEventRepository kbEventRepository;
    @Mock private ConfigAndControlRepository configAndControlRepository;
    @Mock private KbObjectRepository kbObjectRepository;
    @Mock private PubmedResultRepository pubmedResultRepository;

    private GenerateKbForPipelineUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GenerateKbForPipelineUseCase(
                pubtatorPmidReaderRepository,
                pubtatorResultRepository,
                buildKnowledgeBaseRepoWeb,
                kbEventRepository,
                configAndControlRepository,
                kbObjectRepository,
                pubmedResultRepository
        );
    }

    @Test
    @DisplayName("When knowledgeBase does not exist in pubtator_results, should call build-knowledge-base, save knowledgeBase in pubtator_results, and persist in kb_events and kb_objects")
    void execute_withoutCachedKnowledgeBase_shouldCallExternalAndSaveKnowledgeBase() {
        // Arrange
        when(pubtatorPmidReaderRepository.findPubmedIdsByPipelineId(PIPELINE_ID))
                .thenReturn(List.of(PMID));

        PubtatorResult mockPubtatorResult = new PubtatorResult(
                PMID,
                "A Study of Biological Entities",
                "The actual text of study",
                Collections.emptyList(),
                Collections.emptyList()
        );
        when(pubtatorResultRepository.findByPmid(PMID))
                .thenReturn(mockPubtatorResult);

        List<KbEvent> events = List.of(
                new KbEvent(PIPELINE_ID, "IFNG", "positive_correlation", "TLR2", List.of(PMID))
        );
        Map<String, List<String>> synonymsMap = Map.of(
                "IFNG", List.of("IFNG", "IFN-GAMMA")
        );
        Map<String, String> biotypesMap = Map.of(
                "IFNG", "protein"
        );
        GenerateKbResult mockKbResult = new GenerateKbResult(
                events,
                synonymsMap,
                biotypesMap
        );

        when(buildKnowledgeBaseRepoWeb.generateKnowledgeBase(
                eq(PIPELINE_ID),
                eq(PMID),
                eq(mockPubtatorResult.title()),
                eq(mockPubtatorResult.text()),
                eq(mockPubtatorResult.objects()),
                eq(mockPubtatorResult.events())
        )).thenReturn(mockKbResult);

        // Act
        useCase.execute(PIPELINE_ID, USER_ID);

        // Assert
        PubtatorResult.KnowledgeBase expectedKb = new PubtatorResult.KnowledgeBase(
                List.of(new PubtatorResult.KbEventItem("IFNG", "positive_correlation", "TLR2")),
                List.of(new PubtatorResult.KbObjectItem("IFNG", List.of("IFNG", "IFN-GAMMA"), List.of("PROTEIN")))
        );

        verify(pubtatorResultRepository, times(1)).updateKnowledgeBase(PMID, expectedKb);
        verify(kbEventRepository, times(1)).upsert(PIPELINE_ID, "IFNG", "positive_correlation", "TLR2", List.of(PMID));
        verify(kbObjectRepository, times(1)).saveKbObjectItems(PIPELINE_ID, expectedKb.objects());
        verify(pubmedResultRepository, times(1)).deleteByPipelineId(PIPELINE_ID);
        verify(configAndControlRepository, times(1)).updateStep(
                eq(PIPELINE_ID),
                eq(PipelineSteps.BUILD_KNOWLEDGE_BASE),
                eq(Status.COMPLETED),
                eq(USER_ID),
                any()
        );
    }

    @Test
    @DisplayName("When knowledgeBase already exists in pubtator_results, should skip build-knowledge-base call and persist cached data into kb_events and kb_objects")
    void execute_withCachedKnowledgeBase_shouldSkipExternalCallAndUseCachedData() {
        // Arrange
        when(pubtatorPmidReaderRepository.findPubmedIdsByPipelineId(PIPELINE_ID))
                .thenReturn(List.of(PMID));

        PubtatorResult.KnowledgeBase cachedKb = new PubtatorResult.KnowledgeBase(
                List.of(
                        new PubtatorResult.KbEventItem("IFNG", "positive_correlation", "TLR2"),
                        new PubtatorResult.KbEventItem("TLR2", "positive_correlation", "IFNG")
                ),
                List.of(
                        new PubtatorResult.KbObjectItem("IFNG", List.of("IFNG", "IFN-GAMMA"), List.of("PROTEIN"))
                )
        );

        PubtatorResult mockPubtatorResult = new PubtatorResult(
                PMID,
                "A Study of Biological Entities",
                "The actual text of study",
                Collections.emptyList(),
                Collections.emptyList(),
                cachedKb
        );
        when(pubtatorResultRepository.findByPmid(PMID))
                .thenReturn(mockPubtatorResult);

        // Act
        useCase.execute(PIPELINE_ID, USER_ID);

        // Assert
        verifyNoInteractions(buildKnowledgeBaseRepoWeb);
        verify(pubtatorResultRepository, never()).updateKnowledgeBase(anyString(), any());
        verify(kbEventRepository, times(1)).upsert(PIPELINE_ID, "IFNG", "positive_correlation", "TLR2", List.of(PMID));
        verify(kbEventRepository, times(1)).upsert(PIPELINE_ID, "TLR2", "positive_correlation", "IFNG", List.of(PMID));
        verify(kbObjectRepository, times(1)).saveKbObjectItems(PIPELINE_ID, cachedKb.objects());
        verify(pubmedResultRepository, times(1)).deleteByPipelineId(PIPELINE_ID);
        verify(configAndControlRepository, times(1)).updateStep(
                eq(PIPELINE_ID),
                eq(PipelineSteps.BUILD_KNOWLEDGE_BASE),
                eq(Status.COMPLETED),
                eq(USER_ID),
                any()
        );
    }

    @Test
    @DisplayName("When PMIDs list is empty, should finish immediately as completed without external calls")
    void execute_withNoPmids_shouldCompleteEarly() {
        // Arrange
        when(pubtatorPmidReaderRepository.findPubmedIdsByPipelineId(PIPELINE_ID))
                .thenReturn(Collections.emptyList());

        // Act
        useCase.execute(PIPELINE_ID, USER_ID);

        // Assert
        verifyNoInteractions(pubtatorResultRepository, buildKnowledgeBaseRepoWeb, kbObjectRepository, pubmedResultRepository);
        verify(configAndControlRepository, times(1)).updateStep(
                eq(PIPELINE_ID),
                eq(PipelineSteps.BUILD_KNOWLEDGE_BASE),
                eq(Status.COMPLETED),
                eq(USER_ID),
                any()
        );
    }

    @Test
    @DisplayName("When a fatal error occurs, should update status to FAILED")
    void execute_onException_shouldFailStep() {
        // Arrange
        when(pubtatorPmidReaderRepository.findPubmedIdsByPipelineId(PIPELINE_ID))
                .thenThrow(new RuntimeException("Database connection failure"));

        // Act
        useCase.execute(PIPELINE_ID, USER_ID);

        // Assert
        verify(configAndControlRepository, times(1)).updateStep(
                eq(PIPELINE_ID),
                eq(PipelineSteps.BUILD_KNOWLEDGE_BASE),
                eq(Status.FAILED),
                eq(USER_ID),
                any()
        );
    }
}

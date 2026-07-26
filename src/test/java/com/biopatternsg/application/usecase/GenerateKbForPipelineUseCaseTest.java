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
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.repositories.KbEventRepository;
import com.biopatternsg.domain.ports.out.repositories.PubmedResultRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorPmidReaderRepository;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import com.biopatternsg.domain.ports.out.repositories.SynonymRepository;
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
@DisplayName("GenerateKbForPipelineUseCase - Knowledge base and synonym generation process")
class GenerateKbForPipelineUseCaseTest {

    private static final String PIPELINE_ID = "pipeline-123";
    private static final String USER_ID = "user-456";
    private static final String PMID = "12345678";

    @Mock private PubtatorPmidReaderRepository pubtatorPmidReaderRepository;
    @Mock private PubtatorResultRepository pubtatorResultRepository;
    @Mock private BuildKnowledgeBaseRepoWeb buildKnowledgeBaseRepoWeb;
    @Mock private KbEventRepository kbEventRepository;
    @Mock private ConfigAndControlRepository configAndControlRepository;
    @Mock private SynonymRepository synonymRepository;
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
                synonymRepository,
                pubmedResultRepository
        );
    }

    @Test
    @DisplayName("When synonyms are present in external KB response, they should be persisted in SynonymRepository")
    void execute_withSynonyms_shouldPersistSynonyms() {
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

        Map<String, List<String>> synonymsMap = Map.of(
                "BRCA1", List.of("BR1", "RNF53", "Breast Cancer 1")
        );
        GenerateKbResult mockKbResult = new GenerateKbResult(
                Collections.emptyList(),
                synonymsMap
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
        verify(synonymRepository, times(1)).saveSynonyms(PIPELINE_ID, synonymsMap);
        verify(pubmedResultRepository, times(1)).deleteByPipelineId(PIPELINE_ID);
        verify(configAndControlRepository, times(1)).updateStep(
                PIPELINE_ID,
                PipelineSteps.BUILD_KNOWLEDGE_BASE,
                Status.COMPLETED,
                USER_ID
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
        verifyNoInteractions(pubtatorResultRepository, buildKnowledgeBaseRepoWeb, synonymRepository, pubmedResultRepository);
        verify(configAndControlRepository, times(1)).updateStep(
                PIPELINE_ID,
                PipelineSteps.BUILD_KNOWLEDGE_BASE,
                Status.COMPLETED,
                USER_ID
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
                PIPELINE_ID,
                PipelineSteps.BUILD_KNOWLEDGE_BASE,
                Status.FAILED,
                USER_ID
        );
    }
}

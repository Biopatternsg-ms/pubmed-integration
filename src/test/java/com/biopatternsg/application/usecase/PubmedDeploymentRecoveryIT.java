package com.biopatternsg.application.usecase;

import com.biopatternsg.application.services.BiologicalObjectsService;
import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.out.external_repositories.BuildKnowledgeBaseRepoWeb;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.external_repositories.GenerateKbResult;
import com.biopatternsg.domain.ports.out.producers.NcbiQueueSender;
import com.biopatternsg.domain.ports.out.repositories.*;
import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.mongo.PairsCollection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PubmedDeploymentRecoveryIT - Idempotent recovery and clean resumption after deployment")
class PubmedDeploymentRecoveryIT {

    private static final String PIPELINE_ID = "pipe-recovery-123";
    private static final String USER_ID = "tenant-user-1";

    @Mock private BiologicalObjectsService biologicalObjectsService;
    @Mock private PairRepository pairRepository;
    @Mock private ConfigAndControlRepository configAndControlRepository;
    @Mock private NcbiQueueSender ncbiQueueSender;
    @Mock private SearchProgressRepository searchProgressRepository;
    @Mock private PubtatorPmidReaderRepository pubtatorPmidReaderRepository;
    @Mock private PubtatorResultRepository pubtatorResultRepository;
    @Mock private BuildKnowledgeBaseRepoWeb buildKnowledgeBaseRepoWeb;
    @Mock private KbEventRepository kbEventRepository;
    @Mock private KbObjectRepository kbObjectRepository;
    @Mock private PubmedResultRepository pubmedResultRepository;

    @Test
    @DisplayName("Resumption of COMBINATIONS cleans prior partial pairs before regenerating")
    void combinationsStep_cleansPriorPartialPairsOnResumption() {
        BuildPubmedPairsUseCase useCase = new BuildPubmedPairsUseCase(
                biologicalObjectsService, pairRepository, configAndControlRepository, 10
        );

        BiologicalObject obj1 = new BiologicalObject("obj-1", "GeneA", null, null);
        BiologicalObject obj2 = new BiologicalObject("obj-2", "GeneB", null, null);

        when(biologicalObjectsService.getBiologicalObjectsByLevel(PIPELINE_ID, 1))
                .thenReturn(List.of(obj1, obj2));

        // Execute resumption
        useCase.execute(PIPELINE_ID, true, 1, USER_ID);

        // 1. Verify prior data was deleted first (idempotent clean slate)
        verify(pairRepository).deleteByPipelineId(PIPELINE_ID);

        // 2. Verify new pairs saved
        verify(pairRepository, atLeastOnce()).save(any(PairsCollection.class));

        // 3. Verify status updated to COMPLETED with metrics
        verify(configAndControlRepository).updateStep(
                eq(PIPELINE_ID),
                eq(PipelineSteps.COMBINATIONS),
                eq(Status.COMPLETED),
                eq(USER_ID),
                any()
        );
    }

    @Test
    @DisplayName("Resumption of SEARCH_PUBMED re-initializes progress and re-enqueues cleanly")
    void searchPubmedStep_reinitializesProgressOnResumption() {
        SearchPubmedByPairsUseCase useCase = new SearchPubmedByPairsUseCase(
                pairRepository, ncbiQueueSender, searchProgressRepository, configAndControlRepository
        );

        PairsCollection pc = new PairsCollection();
        pc.setPipelineId(PIPELINE_ID);
        PairsCollection.Pair p1 = new PairsCollection.Pair();
        p1.setFirstTerm("GeneA");
        p1.setSecondTerm("GeneB");
        PairsCollection.Pair p2 = new PairsCollection.Pair();
        p2.setFirstTerm("GeneA");
        p2.setSecondTerm("GeneC");
        pc.setPairs(List.of(p1, p2));

        when(pairRepository.findByPipelineId(PIPELINE_ID)).thenReturn(List.of(pc));

        // Execute resumption after queue purge
        useCase.execute(PIPELINE_ID, 100, USER_ID);

        // Verify progress is reset/initialized to 2 terms
        verify(searchProgressRepository).initializeProgress(PIPELINE_ID, 2, USER_ID);

        // Verify clean enqueuing
        verify(ncbiQueueSender, times(2)).send(any());
    }

    @Test
    @DisplayName("Resumption of BUILD_KNOWLEDGE_BASE completes idempotently and clears temporary pubmed results")
    void buildKnowledgeBaseStep_cleansPubmedResultsOnCompletion() {
        GenerateKbForPipelineUseCase useCase = new GenerateKbForPipelineUseCase(
                pubtatorPmidReaderRepository,
                pubtatorResultRepository,
                buildKnowledgeBaseRepoWeb,
                kbEventRepository,
                configAndControlRepository,
                kbObjectRepository,
                pubmedResultRepository
        );

        when(pubtatorPmidReaderRepository.findPubmedIdsByPipelineId(PIPELINE_ID))
                .thenReturn(List.of("12345678"));

        PubtatorResult pubtatorResult = new PubtatorResult(
                "12345678", "Title", "Text",
                Collections.emptyList(), Collections.emptyList()
        );
        when(pubtatorResultRepository.findByPmid("12345678")).thenReturn(pubtatorResult);

        when(buildKnowledgeBaseRepoWeb.generateKnowledgeBase(eq(PIPELINE_ID), eq("12345678"), any(), any(), any(), any()))
                .thenReturn(new GenerateKbResult(Collections.emptyList(), Map.of("GeneA", List.of("SynA")), Map.of("GeneA", "Gene")));

        // Execute KB generation resumption
        useCase.execute(PIPELINE_ID, USER_ID);

        // Verify intermediate pubmed results are pruned
        verify(pubmedResultRepository).deleteByPipelineId(PIPELINE_ID);

        // Verify status marked COMPLETED
        ArgumentCaptor<Map<String, String>> metricsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(configAndControlRepository).updateStep(
                eq(PIPELINE_ID),
                eq(PipelineSteps.BUILD_KNOWLEDGE_BASE),
                eq(Status.COMPLETED),
                eq(USER_ID),
                metricsCaptor.capture()
        );

        assertThat(metricsCaptor.getValue()).containsEntry("pmidsProcessed", "1");
    }
}

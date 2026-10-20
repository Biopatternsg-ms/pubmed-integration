package com.biopatternsg.infrastructure.adapters.out;

import com.biopatternsg.application.usecase.BuildPubmedPairsUseCase;
import com.biopatternsg.application.usecase.SearchPubmedByPairsUseCase;
import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.Status;
import com.biopatternsg.domain.ports.in.*;
import com.biopatternsg.domain.ports.out.external_repositories.ConfigAndControlRepository;
import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import com.biopatternsg.domain.ports.out.repositories.SearchProgressRepository;
import com.biopatternsg.infrastructure.adapters.dtos.BuildPairsRequest;
import com.biopatternsg.infrastructure.adapters.dtos.SearchPairsByPipelineRequest;
import com.biopatternsg.infrastructure.adapters.dtos.SearchPubtatorByPipelineRequest;
import com.biopatternsg.infrastructure.adapters.in.restcontrollers.PubmedController;
import com.biopatternsg.infrastructure.adapters.out.external_repositories.BuildKnowledgeBaseAdapter;
import com.biopatternsg.infrastructure.clients.internal.BuildKnowledgeBaseHttpClient;
import com.biopatternsg.infrastructure.internal_services.QueryBiologicalObjects;
import com.biopatternsg.infrastructure.session.SessionUtils;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PubmedOutboundFaultToleranceTest - Outbound client retries & fail-fast propagation")
class PubmedOutboundFaultToleranceTest {

    @Mock private BuildKnowledgeBaseHttpClient kbHttpClient;
    @Mock private QueryBiologicalObjects queryBiologicalObjects;
    @Mock private ConfigAndControlRepository configAndControlRepository;
    @Mock private BuildPubmedPairs buildPubmedPairs;
    @Mock private SearchPubmedByPairs searchPubmedByPairs;
    @Mock private SearchPubtatorByPmids searchPubtatorByPmids;
    @Mock private GenerateKbForPipeline generateKbForPipeline;
    @Mock private GenerateAlignedObjects generateAlignedObjects;
    @Mock private GetPaginatedKbObjects getPaginatedKbObjects;
    @Mock private GetKbObjectByName getKbObjectByName;
    @Mock private GetKbEventsByTerm getKbEventsByTerm;
    @Mock private GetKbEventsByPipeline getKbEventsByPipeline;
    @Mock private GetAllKbObjectsByPipeline getAllKbObjectsByPipeline;
    @Mock private UpdateKbObjectRoles updateKbObjectRoles;
    @Mock private ResetKbObjectRoles resetKbObjectRoles;
    @Mock private GetAlignedResults getAlignedResults;
    @Mock private GetPublicationsByPmids getPublicationsByPmids;
    @Mock private SessionUtils sessionUtils;

    // Direct synchronous executor for test deterministic execution
    private final Executor directExecutor = Runnable::run;

    @Test
    @DisplayName("BuildKnowledgeBaseAdapter methods are annotated with @Retry(maxRetries=2, delay=1000)")
    void buildKnowledgeBaseAdapter_hasRetryAnnotation() throws NoSuchMethodException {
        Method method = BuildKnowledgeBaseAdapter.class.getMethod(
                "generateKnowledgeBase", String.class, String.class, String.class, String.class, java.util.List.class, java.util.List.class);

        Retry retry = method.getAnnotation(Retry.class);
        assertThat(retry).isNotNull();
        assertThat(retry.maxRetries()).isEqualTo(2);
        assertThat(retry.delay()).isEqualTo(1000);
    }

    @Test
    @DisplayName("BiologicalObjectsRepositoryAdapter methods are annotated with @Retry(maxRetries=2, delay=1000)")
    void biologicalObjectsRepositoryAdapter_hasRetryAnnotation() throws NoSuchMethodException {
        Method m1 = BiologicalObjectsRepositoryAdapter.class.getMethod("biologicalObjectsByPipelineAndLevel", String.class, int.class);
        Method m2 = BiologicalObjectsRepositoryAdapter.class.getMethod("biologicalObjectFatherBrothersAndSons", String.class, String.class);
        Method m3 = BiologicalObjectsRepositoryAdapter.class.getMethod("expertObjectsByPipelineAndLevel", String.class, int.class);

        for (Method m : new Method[]{m1, m2, m3}) {
            Retry retry = m.getAnnotation(Retry.class);
            assertThat(retry).as("Method " + m.getName() + " should have @Retry").isNotNull();
            assertThat(retry.maxRetries()).isEqualTo(2);
            assertThat(retry.delay()).isEqualTo(1000);
        }
    }

    @Test
    @DisplayName("ConfigAndControlRepositoryAdapter methods are annotated with @Retry(maxRetries=2, delay=1000)")
    void configAndControlRepositoryAdapter_hasRetryAnnotation() throws NoSuchMethodException {
        Method m1 = ConfigAndControlRepositoryAdapter.class.getMethod("updateStep", String.class, PipelineSteps.class, Status.class, String.class);
        Method m2 = ConfigAndControlRepositoryAdapter.class.getMethod("updateStep", String.class, PipelineSteps.class, Status.class, String.class, java.util.Map.class);

        for (Method m : new Method[]{m1, m2}) {
            Retry retry = m.getAnnotation(Retry.class);
            assertThat(retry).as("Method " + m.getName() + " should have @Retry").isNotNull();
            assertThat(retry.maxRetries()).isEqualTo(2);
            assertThat(retry.delay()).isEqualTo(1000);
        }
    }

    @Test
    @DisplayName("BuildKnowledgeBaseAdapter propagates ProcessingException after failures")
    void buildKnowledgeBaseAdapter_propagatesException() {
        BuildKnowledgeBaseAdapter adapter = new BuildKnowledgeBaseAdapter(kbHttpClient);
        when(kbHttpClient.generateKb(any())).thenThrow(new ProcessingException("Connection refused"));

        assertThatThrownBy(() -> adapter.generateKnowledgeBase("p1", "123", "title", "text", Collections.emptyList(), Collections.emptyList()))
                .isInstanceOf(ProcessingException.class)
                .hasMessageContaining("Connection refused");
    }

    @Test
    @DisplayName("BiologicalObjectsRepositoryAdapter propagates exceptions when query service fails")
    void biologicalObjectsRepositoryAdapter_propagatesException() {
        BiologicalObjectsRepositoryAdapter adapter = new BiologicalObjectsRepositoryAdapter(queryBiologicalObjects);
        when(queryBiologicalObjects.biologicalObjectsByPipelineAndLevel("p1", 1))
                .thenThrow(new RuntimeException("Remote HTTP 500 error"));

        assertThatThrownBy(() -> adapter.biologicalObjectsByPipelineAndLevel("p1", 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Remote HTTP 500 error");
    }

    @Test
    @DisplayName("PubmedController reports Status.FAILED when buildPairs async execution throws")
    void pubmedController_reportsFailedOnBuildPairsError() {
        PubmedController controller = new PubmedController(
                buildPubmedPairs, searchPubmedByPairs, searchPubtatorByPmids,
                generateKbForPipeline, generateAlignedObjects, getPaginatedKbObjects,
                getKbObjectByName, getKbEventsByTerm, getKbEventsByPipeline,
                getAllKbObjectsByPipeline, updateKbObjectRoles, resetKbObjectRoles,
                getAlignedResults, getPublicationsByPmids, directExecutor,
                sessionUtils, configAndControlRepository
        );

        when(sessionUtils.getUserId()).thenReturn("user-1");
        doThrow(new RuntimeException("Build pairs failed")).when(buildPubmedPairs).execute(any(), anyBoolean(), anyInt(), any());

        controller.buildTreeMesh(new BuildPairsRequest("pipe-1", true, 2));

        verify(configAndControlRepository).updateStep(
                eq("pipe-1"),
                eq(PipelineSteps.COMBINATIONS),
                eq(Status.FAILED),
                eq("user-1"),
                any()
        );
    }

    @Test
    @DisplayName("PubmedController reports Status.FAILED when searchPairs async execution throws")
    void pubmedController_reportsFailedOnSearchPairsError() {
        PubmedController controller = new PubmedController(
                buildPubmedPairs, searchPubmedByPairs, searchPubtatorByPmids,
                generateKbForPipeline, generateAlignedObjects, getPaginatedKbObjects,
                getKbObjectByName, getKbEventsByTerm, getKbEventsByPipeline,
                getAllKbObjectsByPipeline, updateKbObjectRoles, resetKbObjectRoles,
                getAlignedResults, getPublicationsByPmids, directExecutor,
                sessionUtils, configAndControlRepository
        );

        when(sessionUtils.getUserId()).thenReturn("user-1");
        doThrow(new RuntimeException("NCBI enqueuing failed")).when(searchPubmedByPairs).execute(any(), anyInt(), any());

        controller.searchPairs(new SearchPairsByPipelineRequest("pipe-1", 100));

        verify(configAndControlRepository).updateStep(
                eq("pipe-1"),
                eq(PipelineSteps.SEARCH_PUBMED_IDS),
                eq(Status.FAILED),
                eq("user-1"),
                any()
        );
    }

    @Test
    @DisplayName("PubmedController reports Status.FAILED when searchPubtator async execution throws")
    void pubmedController_reportsFailedOnSearchPubtatorError() {
        PubmedController controller = new PubmedController(
                buildPubmedPairs, searchPubmedByPairs, searchPubtatorByPmids,
                generateKbForPipeline, generateAlignedObjects, getPaginatedKbObjects,
                getKbObjectByName, getKbEventsByTerm, getKbEventsByPipeline,
                getAllKbObjectsByPipeline, updateKbObjectRoles, resetKbObjectRoles,
                getAlignedResults, getPublicationsByPmids, directExecutor,
                sessionUtils, configAndControlRepository
        );

        when(sessionUtils.getUserId()).thenReturn("user-1");
        doThrow(new RuntimeException("Pubtator error")).when(searchPubtatorByPmids).execute(any(), any());

        controller.searchPubtatorByPmids(new SearchPubtatorByPipelineRequest("pipe-1"));

        verify(configAndControlRepository).updateStep(
                eq("pipe-1"),
                eq(PipelineSteps.SEARCH_PUBTATOR),
                eq(Status.FAILED),
                eq("user-1"),
                any()
        );
    }
}

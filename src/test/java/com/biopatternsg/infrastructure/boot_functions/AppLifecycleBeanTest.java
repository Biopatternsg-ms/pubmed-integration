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
package com.biopatternsg.infrastructure.boot_functions;

import com.biopatternsg.infrastructure.adapters.dtos.ResumePipelineRequest;
import com.biopatternsg.infrastructure.clients.internal.ConfigAndControlHttpClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppLifecycleBeanTest {

    @Mock
    private ConfigAndControlHttpClient configAndControlHttpClient;

    private AppLifecycleBean appLifecycleBean;

    @BeforeEach
    void setUp() {
        appLifecycleBean = new AppLifecycleBean(configAndControlHttpClient, Runnable::run);
    }

    @Test
    @DisplayName("onStart should invoke resumePipelines with PUBMED_INTEGRATION successfully")
    void testOnStartSuccess() {
        when(configAndControlHttpClient.resumePipelines(any(ResumePipelineRequest.class)))
                .thenReturn(Response.ok().build());

        StartupEvent event = new StartupEvent();
        assertDoesNotThrow(() -> appLifecycleBean.onStart(event));

        ArgumentCaptor<ResumePipelineRequest> captor = ArgumentCaptor.forClass(ResumePipelineRequest.class);
        verify(configAndControlHttpClient, times(1)).resumePipelines(captor.capture());
        assertEquals("PUBMED_INTEGRATION", captor.getValue().microService());
    }

    @Test
    @DisplayName("onStart should catch exception defensively without rethrowing when client fails")
    void testOnStartExceptionHandledDefensively() {
        when(configAndControlHttpClient.resumePipelines(any(ResumePipelineRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        StartupEvent event = new StartupEvent();
        assertDoesNotThrow(() -> appLifecycleBean.onStart(event));

        verify(configAndControlHttpClient, times(1)).resumePipelines(any(ResumePipelineRequest.class));
    }

    @Test
    @DisplayName("onStart with default constructor should not block or throw")
    void testDefaultConstructorExecution() {
        AppLifecycleBean defaultBean = new AppLifecycleBean(configAndControlHttpClient);
        assertNotNull(defaultBean);
        StartupEvent event = new StartupEvent();
        assertDoesNotThrow(() -> defaultBean.onStart(event));
    }
}

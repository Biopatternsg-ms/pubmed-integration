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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AppLifecycleBean {

    private static final Logger LOG = Logger.getLogger(AppLifecycleBean.class);

    private final ConfigAndControlHttpClient configAndControlHttpClient;

    public AppLifecycleBean(@RestClient ConfigAndControlHttpClient configAndControlHttpClient) {
        this.configAndControlHttpClient = configAndControlHttpClient;
    }

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Application starting: initiating autonomous pipeline resumption for PUBMED_INTEGRATION");
        try {
            configAndControlHttpClient.resumePipelines(new ResumePipelineRequest("PUBMED_INTEGRATION"));
            LOG.info("Pipeline resumption request for PUBMED_INTEGRATION dispatched successfully");
        } catch (Exception e) {
            LOG.error("Failed to resume pipelines on application startup for PUBMED_INTEGRATION", e);
        }
    }
}

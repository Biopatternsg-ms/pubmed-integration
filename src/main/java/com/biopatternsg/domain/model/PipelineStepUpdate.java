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
package com.biopatternsg.domain.model;

import java.util.Map;

/**
 * Command encapsulating pipeline step status and optional metrics updates
 * sent to Config and Control.
 */
public record PipelineStepUpdate(
        String pipelineId,
        PipelineSteps step,
        Status status,
        String userId,
        Map<String, String> metrics
) {
    public PipelineStepUpdate {
        metrics = (metrics != null) ? Map.copyOf(metrics) : null;
    }

    public static PipelineStepUpdate of(String pipelineId, PipelineSteps step, Status status, String userId) {
        return new PipelineStepUpdate(pipelineId, step, status, userId, null);
    }

    public static PipelineStepUpdate withMetrics(
            String pipelineId, PipelineSteps step, Status status, String userId, Map<String, String> metrics) {
        return new PipelineStepUpdate(pipelineId, step, status, userId, metrics);
    }
}

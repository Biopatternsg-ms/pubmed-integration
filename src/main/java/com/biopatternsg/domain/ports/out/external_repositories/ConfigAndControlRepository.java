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
package com.biopatternsg.domain.ports.out.external_repositories;

import com.biopatternsg.domain.model.PipelineSteps;
import com.biopatternsg.domain.model.Status;

import java.util.Map;

public interface ConfigAndControlRepository {
    void updateStep(String pipelineId, PipelineSteps step, Status status, String userId);
    void updateStep(String pipelineId, PipelineSteps step, Status status, String userId, Map<String, String> metrics);
}

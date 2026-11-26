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
package com.biopatternsg.application.services;

import com.biopatternsg.domain.model.TermPair;

import java.util.List;
import java.util.Set;

/**
 * Application service responsible for building unique publication term pairs
 * across biological object hierarchy levels.
 */
public interface PubmedPairsBuilderService {

    /**
     * Builds unique term pairs across all root (level-1) biological objects for a pipeline.
     */
    Set<TermPair> buildFirstLevelPairs(String pipelineId, boolean useOnlyPrincipalName);

    /**
     * Builds unique term pair batches for each biological object at the specified level (&gt;= 2)
     * against its family (parent, siblings, and children).
     */
    List<Set<TermPair>> buildHigherLevelPairBatches(String pipelineId, boolean useOnlyPrincipalName, int level);
}

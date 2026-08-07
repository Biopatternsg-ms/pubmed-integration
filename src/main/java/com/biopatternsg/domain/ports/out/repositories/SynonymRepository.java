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
package com.biopatternsg.domain.ports.out.repositories;

import com.biopatternsg.domain.model.PaginatedResult;
import com.biopatternsg.domain.model.PipelineSynonym;

import java.util.List;
import java.util.Map;

import java.util.Optional;

public interface SynonymRepository {
    /**
     * Persists synonym information to the MongoDB collection.
     * If the document (pipelineId, name) exists, appends the new synonyms (preventing duplicates).
     * If it does not exist, creates the document.
     */
    void saveSynonyms(String pipelineId, Map<String, List<String>> synonyms);

    /**
     * Retrieves all synonyms for a given pipelineId, mapped by their name (main ID).
     */
    Map<String, List<String>> findAllByPipelineId(String pipelineId);

    PaginatedResult<PipelineSynonym> findByPipelineId(String pipelineId, int page, int size);

    Optional<PipelineSynonym> findByPipelineIdAndName(String pipelineId, String name);
}

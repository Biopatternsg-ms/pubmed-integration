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

import com.biopatternsg.domain.model.KbObject;
import com.biopatternsg.domain.model.PaginatedResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface KbObjectRepository {
    /**
     * Persists KB object synonym and biotype information to the MongoDB kb_objects collection.
     * If the document (pipelineId, name) exists, appends the new synonyms (preventing duplicates)
     * and sets each synonym's biotype. If it does not exist, creates the document.
     */
    default void saveKbObjects(String pipelineId, Map<String, List<String>> synonyms) {
        saveKbObjects(pipelineId, synonyms, Collections.emptyMap());
    }

    void saveKbObjects(String pipelineId, Map<String, List<String>> synonyms, Map<String, String> biotypes);

    /**
     * Retrieves all synonyms for a given pipelineId, mapped by their name (main ID).
     */
    Map<String, List<String>> findAllSynonymsByPipelineId(String pipelineId);

    PaginatedResult<KbObject> findByPipelineId(String pipelineId, int page, int size);

    List<KbObject> findAllByPipelineId(String pipelineId);

    Optional<KbObject> findByPipelineIdAndName(String pipelineId, String name);

    void updateRoles(String pipelineId, Map<String, List<String>> roles);

    void resetRoles(String pipelineId);
}

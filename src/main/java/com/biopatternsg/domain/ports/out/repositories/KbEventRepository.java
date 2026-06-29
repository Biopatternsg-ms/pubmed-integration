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

import com.biopatternsg.domain.model.KbEvent;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia de eventos de la base de conocimiento.
 */
public interface KbEventRepository {

    /**
     * Busca un evento por su pipelineId y su tripleta (first, relation, second).
     *
     * @return el evento encontrado, o empty si no existe.
     */
    Optional<KbEvent> findByRelation(String pipelineId, String first, String relation, String second);

    /**
     * Realiza un registro o actualización atómica (upsert) en MongoDB.
     * Si la combinación (pipelineId, first, relation, second) no existe, crea el documento con sus pubmedIds.
     * Si ya existe, añade únicamente los pubmedIds que no estén en la lista.
     */
    void upsert(String pipelineId, String first, String relation, String second, List<String> pubmedIds);
}


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
package com.biopatternsg.infrastructure.adapters.out;

import com.biopatternsg.domain.model.KbEvent;
import com.biopatternsg.domain.ports.out.repositories.KbEventRepository;
import com.biopatternsg.mongo.KbEventCollection;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class KbEventRepositoryAdapter implements KbEventRepository, PanacheMongoRepository<KbEventCollection> {

    @Override
    public Optional<KbEvent> findByRelation(String pipelineId, String first, String relation, String second) {
        try {
            KbEventCollection doc = find("pipelineId = ?1 and first = ?2 and relation = ?3 and second = ?4",
                    pipelineId, first, relation, second).firstResult();
            return Optional.ofNullable(doc).map(this::toDomain);
        } catch (Exception e) {
            log.error("Error finding KbEvent [{}, {},{},{}]: {}", pipelineId, first, relation, second, e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public void upsert(String pipelineId, String first, String relation, String second, List<String> pubmedIds) {
        try {
            if (pubmedIds == null || pubmedIds.isEmpty()) {
                return;
            }
            mongoCollection().updateOne(
                    Filters.and(
                            Filters.eq("pipelineId", pipelineId),
                            Filters.eq("first", first),
                            Filters.eq("relation", relation),
                            Filters.eq("second", second)
                    ),
                    Updates.addEachToSet("pubmedIds", pubmedIds),
                    new com.mongodb.client.model.UpdateOptions().upsert(true)
            );
            log.debug("KbEvent upserted: [{}, [{},{},{}]] with PMIDs {}", pipelineId, first, relation, second, pubmedIds);
        } catch (Exception e) {
            log.error("Error upserting KbEvent [{}, [{},{},{}]]: {}", pipelineId, first, relation, second, e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    private KbEvent toDomain(KbEventCollection doc) {
        return new KbEvent(
                doc.getPipelineId(),
                doc.getFirst(),
                doc.getRelation(),
                doc.getSecond(),
                doc.getPubmedIds() != null ? doc.getPubmedIds() : new ArrayList<>()
        );
    }
}

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
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class KbEventRepositoryImpl implements KbEventRepository, PanacheMongoRepository<KbEventCollection> {

    @Override
    public Optional<KbEvent> findByRelation(String first, String relation, String second) {
        try {
            KbEventCollection doc = find("first = ?1 and relation = ?2 and second = ?3",
                    first, relation, second).firstResult();
            return Optional.ofNullable(doc).map(this::toDomain);
        } catch (Exception e) {
            log.error("Error finding KbEvent [{},{},{}]: {}", first, relation, second, e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public void save(KbEvent event) {
        try {
            KbEventCollection doc = new KbEventCollection();
            doc.setFirst(event.first());
            doc.setRelation(event.relation());
            doc.setSecond(event.second());
            doc.setPubmedIds(new ArrayList<>(event.pubmedIds()));
            persist(doc);
            log.debug("KbEvent saved: [{},{},{}]", event.first(), event.relation(), event.second());
        } catch (Exception e) {
            log.error("Error saving KbEvent [{},{},{}]: {}", event.first(), event.relation(), event.second(), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public void addPubmedId(String first, String relation, String second, String pubmedId) {
        try {
            mongoCollection().updateOne(
                    Filters.and(
                            Filters.eq("first", first),
                            Filters.eq("relation", relation),
                            Filters.eq("second", second)
                    ),
                    Updates.addToSet("pubmedIds", pubmedId)
            );
            log.debug("PubmedId [{}] added to KbEvent [{},{},{}]", pubmedId, first, relation, second);
        } catch (Exception e) {
            log.error("Error adding pubmedId [{}] to KbEvent [{},{},{}]: {}",
                    pubmedId, first, relation, second, e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    private KbEvent toDomain(KbEventCollection doc) {
        return new KbEvent(
                doc.getFirst(),
                doc.getRelation(),
                doc.getSecond(),
                doc.getPubmedIds() != null ? doc.getPubmedIds() : new ArrayList<>()
        );
    }
}

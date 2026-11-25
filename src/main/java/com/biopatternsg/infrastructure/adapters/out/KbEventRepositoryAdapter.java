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
import com.biopatternsg.domain.model.KbEventKey;
import com.biopatternsg.domain.ports.out.repositories.KbEventRepository;
import com.biopatternsg.domain.util.BiologicalTermNormalizer;
import com.biopatternsg.domain.util.LogSanitizer;
import com.biopatternsg.infrastructure.mongo.KbEventCollection;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class KbEventRepositoryAdapter implements KbEventRepository, PanacheMongoRepository<KbEventCollection> {

    @Override
    public Optional<KbEvent> findByRelation(KbEventKey key) {
        try {
            String cleanFirst = BiologicalTermNormalizer.extractSuffix(key.first());
            String cleanSecond = BiologicalTermNormalizer.extractSuffix(key.second());
            KbEventCollection doc = find("pipelineId = ?1 and first = ?2 and relation = ?3 and second = ?4",
                    key.pipelineId(), cleanFirst, key.relation(), cleanSecond).firstResult();
            return Optional.ofNullable(doc).map(this::toDomain);
        } catch (Exception e) {
            log.error("Error finding KbEvent [{}, {},{},{}]: {}",
                    LogSanitizer.sanitize(key.pipelineId()), LogSanitizer.sanitize(key.first()),
                    LogSanitizer.sanitize(key.relation()), LogSanitizer.sanitize(key.second()), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public void upsert(KbEvent event) {
        try {
            List<String> pubmedIds = event.pubmedIds();
            if (pubmedIds == null || pubmedIds.isEmpty()) {
                return;
            }
            String cleanFirst = BiologicalTermNormalizer.extractSuffix(event.first());
            String cleanSecond = BiologicalTermNormalizer.extractSuffix(event.second());

            mongoCollection().updateOne(
                    Filters.and(
                            Filters.eq("pipelineId", event.pipelineId()),
                            Filters.eq("first", cleanFirst),
                            Filters.eq("relation", event.relation()),
                            Filters.eq("second", cleanSecond)
                    ),
                    Updates.addEachToSet("pubmedIds", pubmedIds),
                    new com.mongodb.client.model.UpdateOptions().upsert(true)
            );
            log.debug("KbEvent upserted: [{}, [{},{},{}]] with PMIDs {}",
                    LogSanitizer.sanitize(event.pipelineId()), LogSanitizer.sanitize(cleanFirst),
                    LogSanitizer.sanitize(event.relation()), LogSanitizer.sanitize(cleanSecond), pubmedIds);
        } catch (Exception e) {
            log.error("Error upserting KbEvent [{}, [{},{},{}]]: {}",
                    LogSanitizer.sanitize(event.pipelineId()), LogSanitizer.sanitize(event.first()),
                    LogSanitizer.sanitize(event.relation()), LogSanitizer.sanitize(event.second()), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public List<KbEvent> findByPipelineIdAndTerm(String pipelineId, String term) {
        try {
            if (term == null || term.isBlank()) {
                return List.of();
            }
            String cleanTerm = BiologicalTermNormalizer.extractSuffix(term);
            String regex = "^" + java.util.regex.Pattern.quote(cleanTerm) + "$";
            return find("{'pipelineId': ?1, '$or': [{'first': {'$regex': ?2, '$options': 'i'}}, {'second': {'$regex': ?2, '$options': 'i'}}]}", pipelineId, regex)
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding KbEvents for pipelineId=[{}] and term=[{}]: {}",
                    LogSanitizer.sanitize(pipelineId), LogSanitizer.sanitize(term), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public List<KbEvent> findByPipelineId(String pipelineId) {
        try {
            if (pipelineId == null || pipelineId.isBlank()) {
                return List.of();
            }
            return find("pipelineId", pipelineId)
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding all KbEvents for pipelineId=[{}]: {}",
                    LogSanitizer.sanitize(pipelineId), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    private KbEvent toDomain(KbEventCollection doc) {
        return new KbEvent(
                doc.id != null ? doc.id.toHexString() : null,
                doc.getPipelineId(),
                doc.getFirst(),
                doc.getRelation(),
                doc.getSecond(),
                doc.getPubmedIds() != null ? doc.getPubmedIds() : new ArrayList<>()
        );
    }
}

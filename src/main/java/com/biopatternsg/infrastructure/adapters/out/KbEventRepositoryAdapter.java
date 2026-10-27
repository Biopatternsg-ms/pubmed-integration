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
import com.biopatternsg.domain.util.LogSanitizer;
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
import java.util.Set;
import java.util.stream.Collectors;

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
            log.error("Error finding KbEvent [{}, {},{},{}]: {}",
                    LogSanitizer.sanitize(pipelineId), LogSanitizer.sanitize(first),
                    LogSanitizer.sanitize(relation), LogSanitizer.sanitize(second), e.getMessage(), e);
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
            log.debug("KbEvent upserted: [{}, [{},{},{}]] with PMIDs {}",
                    LogSanitizer.sanitize(pipelineId), LogSanitizer.sanitize(first),
                    LogSanitizer.sanitize(relation), LogSanitizer.sanitize(second), pubmedIds);
        } catch (Exception e) {
            log.error("Error upserting KbEvent [{}, [{},{},{}]]: {}",
                    LogSanitizer.sanitize(pipelineId), LogSanitizer.sanitize(first),
                    LogSanitizer.sanitize(relation), LogSanitizer.sanitize(second), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public List<KbEvent> findByPipelineIdAndTerm(String pipelineId, String term) {
        try {
            if (term == null || term.isBlank()) {
                return List.of();
            }
            String cleanTerm = term.trim();
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

    @Override
    public List<KbEvent> findByPipelineIdAndRestriction(String pipelineId, String restrictionLevel, List<String> alignedObjects) {
        try {
            if (pipelineId == null || pipelineId.isBlank()) {
                return List.of();
            }

            List<KbEvent> allEvents = findByPipelineId(pipelineId);
            if (allEvents.isEmpty()) {
                return List.of();
            }

            if (restrictionLevel == null || restrictionLevel.isBlank()
                    || "UNRESTRICTED".equalsIgnoreCase(restrictionLevel)
                    || "NO_RESTRICTED".equalsIgnoreCase(restrictionLevel)) {
                return allEvents;
            }

            if (alignedObjects == null || alignedObjects.isEmpty()) {
                return List.of();
            }

            Set<String> alignedSet = alignedObjects.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toUpperCase())
                    .collect(Collectors.toSet());

            if (alignedSet.isEmpty()) {
                return List.of();
            }

            if ("VERY_RESTRICTED".equalsIgnoreCase(restrictionLevel)) {
                return allEvents.stream()
                        .filter(e -> e.first() != null && e.second() != null
                                && alignedSet.contains(e.first().trim().toUpperCase())
                                && alignedSet.contains(e.second().trim().toUpperCase()))
                        .toList();
            }

            if ("RESTRICTED".equalsIgnoreCase(restrictionLevel)) {
                return allEvents.stream()
                        .filter(e -> (e.first() != null && alignedSet.contains(e.first().trim().toUpperCase()))
                                || (e.second() != null && alignedSet.contains(e.second().trim().toUpperCase())))
                        .toList();
            }

            return allEvents;
        } catch (Exception e) {
            log.error("Error finding KbEvents for pipelineId=[{}] and restrictionLevel=[{}]: {}",
                    LogSanitizer.sanitize(pipelineId), LogSanitizer.sanitize(restrictionLevel), e.getMessage(), e);
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

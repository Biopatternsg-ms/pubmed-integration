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

import com.biopatternsg.domain.model.KbObject;
import com.biopatternsg.domain.model.PaginatedResult;
import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.ports.out.repositories.KbObjectRepository;
import com.biopatternsg.domain.util.LogSanitizer;
import com.biopatternsg.mongo.KbObjectCollection;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class KbObjectRepositoryAdapter implements KbObjectRepository, PanacheMongoRepository<KbObjectCollection> {

    @Override
    public void saveKbObjects(String pipelineId, Map<String, List<String>> synonyms, Map<String, String> biotypes) {
        if (synonyms == null || synonyms.isEmpty()) {
            return;
        }
        List<PubtatorResult.KbObjectItem> objects = PubtatorResult.KnowledgeBase.buildObjects(synonyms, biotypes);
        saveKbObjectItems(pipelineId, objects);
    }

    @Override
    public void saveKbObjectItems(String pipelineId, List<PubtatorResult.KbObjectItem> objects) {
        if (objects == null || objects.isEmpty()) {
            return;
        }

        for (PubtatorResult.KbObjectItem obj : objects) {
            String name = obj.name();
            List<String> synonymList = obj.synonyms();

            if (name == null || name.isBlank() || synonymList == null || synonymList.isEmpty()) {
                continue;
            }

            try {
                List<Bson> updates = new ArrayList<>();
                updates.add(Updates.addEachToSet("synonyms", synonymList));

                List<String> biotypes = obj.biotypes();
                if (biotypes != null && !biotypes.isEmpty()) {
                    List<String> normalizedBiotypes = biotypes.stream()
                            .filter(b -> b != null && !b.isBlank())
                            .map(b -> b.trim().toUpperCase())
                            .distinct()
                            .toList();
                    if (!normalizedBiotypes.isEmpty()) {
                        updates.add(Updates.addEachToSet("biotypes", new ArrayList<>(normalizedBiotypes)));
                    }
                }

                Bson updateOperation = updates.size() == 1
                        ? updates.get(0)
                        : Updates.combine(updates);

                mongoCollection().updateOne(
                        Filters.and(
                                Filters.eq("pipelineId", pipelineId),
                                Filters.eq("name", name)
                        ),
                        updateOperation,
                        new UpdateOptions().upsert(true)
                );
                log.debug("KB object upserted for pipelineId=[{}], name=[{}]: synonyms={}",
                        LogSanitizer.sanitize(pipelineId), LogSanitizer.sanitize(name), synonymList);
            } catch (Exception e) {
                log.error("Error upserting KB object for [{}, {}]: {}",
                        LogSanitizer.sanitize(pipelineId), LogSanitizer.sanitize(name), e.getMessage(), e);
                throw new InternalServerError(e);
            }
        }
    }

    @Override
    public Map<String, List<String>> findAllSynonymsByPipelineId(String pipelineId) {
        try {
            return find("pipelineId", pipelineId)
                    .stream()
                    .collect(Collectors.toMap(
                            KbObjectCollection::getName,
                            KbObjectCollection::getSynonyms,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Error finding all KB object synonyms for pipelineId=[{}]: {}",
                    LogSanitizer.sanitize(pipelineId), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public PaginatedResult<KbObject> findByPipelineId(String pipelineId, int page, int size) {
        try {
            var query = find("pipelineId", pipelineId);
            long totalItems = query.count();
            int totalPages = (int) Math.ceil((double) totalItems / size);

            List<KbObject> items = query.page(page, size)
                    .stream()
                    .map(col -> new KbObject(
                            col.getName(),
                            col.getSynonyms() != null ? col.getSynonyms() : Collections.emptyList(),
                            col.getBiotypes() != null ? col.getBiotypes() : Collections.emptyList(),
                            col.getRoles() != null ? col.getRoles() : Collections.emptyList()
                    ))
                    .collect(Collectors.toList());

            return new PaginatedResult<>(items, totalItems, totalPages, page, size);
        } catch (Exception e) {
            log.error("Error finding paginated KB objects for pipelineId=[{}]: {}", LogSanitizer.sanitize(pipelineId), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public List<KbObject> findAllByPipelineId(String pipelineId) {
        try {
            return find("pipelineId", pipelineId)
                    .stream()
                    .map(col -> new KbObject(
                            col.getName(),
                            col.getSynonyms() != null ? col.getSynonyms() : Collections.emptyList(),
                            col.getBiotypes() != null ? col.getBiotypes() : Collections.emptyList(),
                            col.getRoles() != null ? col.getRoles() : Collections.emptyList()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding all KB objects for pipelineId=[{}]: {}", LogSanitizer.sanitize(pipelineId), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public Optional<KbObject> findByPipelineIdAndName(String pipelineId, String name) {
        try {
            if (name == null || name.isBlank()) {
                return Optional.empty();
            }
            String cleanName = name.trim();
            var exactResult = find("pipelineId = ?1 and name = ?2", pipelineId, cleanName).firstResultOptional();
            if (exactResult.isPresent()) {
                return exactResult.map(col -> new KbObject(
                        col.getName(),
                        col.getSynonyms() != null ? col.getSynonyms() : Collections.emptyList(),
                        col.getBiotypes() != null ? col.getBiotypes() : Collections.emptyList(),
                        col.getRoles() != null ? col.getRoles() : Collections.emptyList()
                ));
            }

            String regex = "^" + java.util.regex.Pattern.quote(cleanName) + "$";
            return find("{'pipelineId': ?1, '$or': [{'name': {'$regex': ?2, '$options': 'i'}}, {'synonyms': {'$regex': ?2, '$options': 'i'}}]}", pipelineId, regex)
                    .firstResultOptional()
                    .map(col -> new KbObject(
                            col.getName(),
                            col.getSynonyms() != null ? col.getSynonyms() : Collections.emptyList(),
                            col.getBiotypes() != null ? col.getBiotypes() : Collections.emptyList(),
                            col.getRoles() != null ? col.getRoles() : Collections.emptyList()
                    ));
        } catch (Exception e) {
            log.error("Error finding KB object for pipelineId=[{}] and name=[{}]: {}",
                    LogSanitizer.sanitize(pipelineId), LogSanitizer.sanitize(name), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public void updateRoles(String pipelineId, Map<String, List<String>> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        try {
            List<WriteModel<KbObjectCollection>> writes = new ArrayList<>(roles.size());
            for (Map.Entry<String, List<String>> entry : roles.entrySet()) {
                String name = entry.getKey();
                List<String> roleList = entry.getValue() != null ? entry.getValue() : Collections.emptyList();
                writes.add(new UpdateOneModel<>(
                        Filters.and(
                                Filters.eq("pipelineId", pipelineId),
                                Filters.eq("name", name)
                        ),
                        Updates.set("roles", roleList),
                        new UpdateOptions().upsert(true)
                ));
            }
            mongoCollection().bulkWrite(writes, new BulkWriteOptions().ordered(false));
            log.info("Bulk updated roles for {} objects in pipelineId=[{}]", roles.size(), LogSanitizer.sanitize(pipelineId));
        } catch (Exception e) {
            log.error("Error updating roles for pipelineId=[{}]: {}", LogSanitizer.sanitize(pipelineId), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public void resetRoles(String pipelineId) {
        if (pipelineId == null || pipelineId.isBlank()) {
            return;
        }
        try {
            mongoCollection().updateMany(
                    Filters.eq("pipelineId", pipelineId),
                    Updates.unset("roles")
            );
            log.info("Reset roles for all objects in pipelineId=[{}]", LogSanitizer.sanitize(pipelineId));
        } catch (Exception e) {
            log.error("Error resetting roles for pipelineId=[{}]: {}", LogSanitizer.sanitize(pipelineId), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }
}

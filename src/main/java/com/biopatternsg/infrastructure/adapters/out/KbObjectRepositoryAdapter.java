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
import com.biopatternsg.domain.ports.out.repositories.KbObjectRepository;
import com.biopatternsg.mongo.KbObjectCollection;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;

import java.util.ArrayList;
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

        for (Map.Entry<String, List<String>> entry : synonyms.entrySet()) {
            String name = entry.getKey();
            List<String> synonymList = entry.getValue();

            if (synonymList == null || synonymList.isEmpty()) {
                continue;
            }

            try {
                List<Bson> updates = new ArrayList<>();
                updates.add(Updates.addEachToSet("synonyms", synonymList));

                if (biotypes != null && !biotypes.isEmpty()) {
                    String mainBiotype = biotypes.get(name);
                    for (String syn : synonymList) {
                        if (syn == null || syn.isBlank()) {
                            continue;
                        }
                        String synBiotype = biotypes.getOrDefault(syn, mainBiotype);
                        if (synBiotype != null && !synBiotype.isBlank()) {
                            String safeKey = syn.replace(".", "\uFF0E");
                            updates.add(Updates.set("biotypes." + safeKey, synBiotype));
                        }
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
                log.debug("KB object upserted for pipelineId=[{}], name=[{}]: synonyms={}", pipelineId, name, synonymList);
            } catch (Exception e) {
                log.error("Error upserting KB object for [{}, {}]: {}", pipelineId, name, e.getMessage(), e);
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
            log.error("Error finding all KB object synonyms for pipelineId=[{}]: {}", pipelineId, e.getMessage(), e);
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
                    .map(col -> new KbObject(col.getName(), col.getSynonyms(), col.getBiotypes()))
                    .collect(Collectors.toList());

            return new PaginatedResult<>(items, totalItems, totalPages, page, size);
        } catch (Exception e) {
            log.error("Error finding paginated KB objects for pipelineId=[{}]: {}", pipelineId, e.getMessage(), e);
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
                return exactResult.map(col -> new KbObject(col.getName(), col.getSynonyms(), col.getBiotypes()));
            }

            String regex = "^" + java.util.regex.Pattern.quote(cleanName) + "$";
            return find("{'pipelineId': ?1, '$or': [{'name': {'$regex': ?2, '$options': 'i'}}, {'synonyms': {'$regex': ?2, '$options': 'i'}}]}", pipelineId, regex)
                    .firstResultOptional()
                    .map(col -> new KbObject(col.getName(), col.getSynonyms(), col.getBiotypes()));
        } catch (Exception e) {
            log.error("Error finding KB object for pipelineId=[{}] and name=[{}]: {}", pipelineId, name, e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }
}

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

import com.biopatternsg.domain.model.PaginatedResult;
import com.biopatternsg.domain.model.PipelineSynonym;
import com.biopatternsg.domain.ports.out.repositories.SynonymRepository;
import com.biopatternsg.mongo.SynonymCollection;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class SynonymRepositoryAdapter implements SynonymRepository, PanacheMongoRepository<SynonymCollection> {

    @Override
    public void saveSynonyms(String pipelineId, Map<String, List<String>> synonyms) {
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
                mongoCollection().updateOne(
                        Filters.and(
                                Filters.eq("pipelineId", pipelineId),
                                Filters.eq("name", name)
                        ),
                        Updates.addEachToSet("synonyms", synonymList),
                        new com.mongodb.client.model.UpdateOptions().upsert(true)
                );
                log.debug("Synonyms upserted for pipelineId=[{}], name=[{}]: {}", pipelineId, name, synonymList);
            } catch (Exception e) {
                log.error("Error upserting synonyms for [{}, {}]: {}", pipelineId, name, e.getMessage(), e);
                throw new InternalServerError(e);
            }
        }
    }

    @Override
    public Map<String, List<String>> findAllByPipelineId(String pipelineId) {
        try {
            return find("pipelineId", pipelineId)
                    .stream()
                    .collect(Collectors.toMap(
                            SynonymCollection::getName,
                            SynonymCollection::getSynonyms,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Error finding all synonyms for pipelineId=[{}]: {}", pipelineId, e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public PaginatedResult<PipelineSynonym> findByPipelineId(String pipelineId, int page, int size) {
        try {
            var query = find("pipelineId", pipelineId);
            long totalItems = query.count();
            int totalPages = (int) Math.ceil((double) totalItems / size);

            List<PipelineSynonym> items = query.page(page, size)
                    .stream()
                    .map(col -> new PipelineSynonym(col.getName(), col.getSynonyms()))
                    .collect(Collectors.toList());

            return new PaginatedResult<>(items, totalItems, totalPages, page, size);
        } catch (Exception e) {
            log.error("Error finding paginated synonyms for pipelineId=[{}]: {}", pipelineId, e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

    @Override
    public Optional<PipelineSynonym> findByPipelineIdAndName(String pipelineId, String name) {
        try {
            if (name == null || name.isBlank()) {
                return Optional.empty();
            }
            String cleanName = name.trim();
            var exactResult = find("pipelineId = ?1 and name = ?2", pipelineId, cleanName).firstResultOptional();
            if (exactResult.isPresent()) {
                return exactResult.map(col -> new PipelineSynonym(col.getName(), col.getSynonyms()));
            }

            String regex = "^" + java.util.regex.Pattern.quote(cleanName) + "$";
            return find("{'pipelineId': ?1, '$or': [{'name': {'$regex': ?2, '$options': 'i'}}, {'synonyms': {'$regex': ?2, '$options': 'i'}}]}", pipelineId, regex)
                    .firstResultOptional()
                    .map(col -> new PipelineSynonym(col.getName(), col.getSynonyms()));
        } catch (Exception e) {
            log.error("Error finding synonyms for pipelineId=[{}] and name=[{}]: {}", pipelineId, name, e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }
}

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

import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import com.biopatternsg.infrastructure.mapper.PubtatorResultMapper;
import com.biopatternsg.infrastructure.mongo.PubtatorResultCollection;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class PubtatorResultRepositoryAdapter implements PubtatorResultRepository, PanacheMongoRepository<PubtatorResultCollection> {

    @Override
    public void save(PubtatorResult result) {
        try {
            persist(PubtatorResultMapper.modelToCollection(result));
        } catch (Exception e) {
            if (e.getMessage() == null || !e.getMessage().contains("E11000")) {
                throw new InternalServerError(e);
            }
        }
    }

    @Override
    public void saveAll(List<PubtatorResult> results) {
        try {
            persist(PubtatorResultMapper.modelToCollectionList(results));
        } catch (Exception e) {
            if (e.getMessage() == null || !e.getMessage().contains("E11000")) {
                throw new InternalServerError(e);
            }
        }
    }

    @Override
    public void updateKnowledgeBase(String pmid, PubtatorResult.KnowledgeBase knowledgeBase) {
        if (pmid == null || pmid.isBlank() || knowledgeBase == null) {
            return;
        }
        try {
            mongoCollection().updateOne(
                    Filters.eq("pmid", pmid),
                    Updates.set("knowledgeBase", PubtatorResultMapper.knowledgeBaseToCollection(knowledgeBase))
            );
        } catch (Exception e) {
            throw new InternalServerError(e);
        }
    }

    @Override
    public List<String> findExistingPmids(List<String> pmids) {
        if (pmids == null || pmids.isEmpty()) {
            return Collections.emptyList();
        }
        return find("{'pmid': {'$in': ?1}}", pmids).stream()
                .map(PubtatorResultCollection::getPmid)
                .toList();
    }

    @Override
    public PubtatorResult findByPmid(String pmid) {
        return PubtatorResultMapper.collectionToModel(find("pmid", pmid).firstResult());
    }

    @Override
    public List<PubtatorResult> findByPmids(List<String> pmids) {
        if (pmids == null || pmids.isEmpty()) {
            return Collections.emptyList();
        }
        return PubtatorResultMapper.collectionToModelList(
                find("{'pmid': {'$in': ?1}}", pmids).list());
    }
}

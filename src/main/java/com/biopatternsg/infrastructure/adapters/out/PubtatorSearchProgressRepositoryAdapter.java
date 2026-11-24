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

import com.biopatternsg.domain.ports.out.repositories.PubtatorSearchProgressRepository;
import com.biopatternsg.mongo.PubtatorSearchProgressCollection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;

@ApplicationScoped
public class PubtatorSearchProgressRepositoryAdapter
        implements PubtatorSearchProgressRepository, PanacheMongoRepository<PubtatorSearchProgressCollection> {

    @Override
    public void initializeProgress(String pipelineId, int totalCount, String userId) {
        // Delete any existing progress document for the pipeline to avoid duplicate/stale records
        deleteByPipelineId(pipelineId);

        PubtatorSearchProgressCollection progress = new PubtatorSearchProgressCollection();
        progress.setPipelineId(pipelineId);
        progress.setCompletedCount(0);
        progress.setTotalCount(totalCount);
        progress.setUserId(userId);
        persist(progress);
    }

    @Override
    public PubtatorSearchProgressCollection incrementAndGet(String pipelineId) {
        MongoCollection<PubtatorSearchProgressCollection> collection = mongoCollection();
        Document filter = new Document("pipelineId", pipelineId);
        Document update = new Document("$inc", new Document("completedCount", 1));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
        return collection.findOneAndUpdate(filter, update, options);
    }

    @Override
    public void deleteByPipelineId(String pipelineId) {
        delete("pipelineId", pipelineId);
    }
}

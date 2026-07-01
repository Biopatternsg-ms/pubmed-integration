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

import com.biopatternsg.domain.model.AlignedResult;
import com.biopatternsg.domain.ports.out.repositories.AlignedResultRepository;
import com.biopatternsg.mongo.AlignedResultCollection;
import com.biopatternsg.mongo.AlignedAsEmbedded;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class AlignedResultRepositoryAdapter implements AlignedResultRepository, PanacheMongoRepository<AlignedResultCollection> {

    @Override
    public void save(AlignedResult alignedResult) {
        if (alignedResult == null) {
            return;
        }

        try {
            AlignedResultCollection entity = find("pipelineId", alignedResult.pipelineId()).firstResult();
            boolean isNew = false;
            if (entity == null) {
                entity = new AlignedResultCollection();
                entity.setPipelineId(alignedResult.pipelineId());
                isNew = true;
            }

            entity.setAligned(alignedResult.aligned());
            entity.setNoAligned(alignedResult.noAligned());

            List<AlignedAsEmbedded> embeddedList = alignedResult.alignedAs().stream()
                    .map(a -> new AlignedAsEmbedded(a.expertObjectName(), a.alternativeIds()))
                    .collect(Collectors.toList());
            entity.setAlignedAs(embeddedList);

            entity.setAlignedAndAlternatives(alignedResult.alignedAndAlternatives());

            if (isNew) {
                persist(entity);
            } else {
                update(entity);
            }
            log.info("AlignedResult successfully saved/updated for pipelineId=[{}]", alignedResult.pipelineId());
        } catch (Exception e) {
            log.error("Error saving AlignedResult for pipelineId=[{}]: {}", alignedResult.pipelineId(), e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }
}

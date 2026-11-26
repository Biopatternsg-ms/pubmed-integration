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
package com.biopatternsg.infrastructure.mapper;

import com.biopatternsg.domain.model.AlignedAs;
import com.biopatternsg.domain.model.AlignedResult;
import com.biopatternsg.domain.model.AlignedResultSummary;
import com.biopatternsg.infrastructure.mongo.AlignedAsEmbedded;
import com.biopatternsg.infrastructure.mongo.AlignedResultCollection;

import java.util.Collections;
import java.util.List;

/**
 * Stateless pure mapper for {@link AlignedResult}, {@link AlignedResultSummary}, and {@link AlignedResultCollection}.
 */
public final class AlignedResultMapper {

    private AlignedResultMapper() {
    }

    public static AlignedResultCollection modelToCollection(AlignedResult alignedResult, AlignedResultCollection target) {
        if (alignedResult == null) {
            return target;
        }
        AlignedResultCollection entity = target != null ? target : new AlignedResultCollection();
        entity.setPipelineId(alignedResult.pipelineId());
        entity.setAligned(alignedResult.aligned());
        entity.setNoAligned(alignedResult.noAligned());
        List<AlignedAsEmbedded> embeddedList = alignedResult.alignedAs() == null
                ? Collections.emptyList()
                : alignedResult.alignedAs().stream()
                        .map(a -> new AlignedAsEmbedded(a.expertObjectName(), a.alternativeIds()))
                        .toList();
        entity.setAlignedAs(embeddedList);
        entity.setAlignedAndAlternatives(alignedResult.alignedAndAlternatives());
        return entity;
    }

    public static AlignedResultSummary collectionToSummary(AlignedResultCollection entity) {
        if (entity == null) {
            return null;
        }
        List<AlignedAs> alignedAsList = entity.getAlignedAs() == null
                ? Collections.emptyList()
                : entity.getAlignedAs().stream()
                        .map(a -> new AlignedAs(a.getExpertObjectName(), a.getAlternativeIds()))
                        .toList();

        List<String> aligned = entity.getAligned() != null ? entity.getAligned() : Collections.emptyList();
        List<String> noAligned = entity.getNoAligned() != null ? entity.getNoAligned() : Collections.emptyList();

        return new AlignedResultSummary(entity.getPipelineId(), aligned, noAligned, alignedAsList);
    }
}

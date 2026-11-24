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

import com.biopatternsg.domain.model.PipelinePairsBatch;
import com.biopatternsg.domain.model.TermPair;
import com.biopatternsg.infrastructure.mongo.PairsCollection;

import java.util.List;
import java.util.Objects;

public final class PairMapper {

    private PairMapper() {
    }

    public static PipelinePairsBatch collectionToModel(PairsCollection collection) {
        if (collection == null) {
            return null;
        }
        List<TermPair> pairs = collection.getPairs() == null
                ? List.of()
                : collection.getPairs().stream()
                        .filter(Objects::nonNull)
                        .map(PairMapper::pairToModel)
                        .toList();
        return new PipelinePairsBatch(collection.getPipelineId(), pairs);
    }

    public static List<PipelinePairsBatch> collectionListToModel(List<PairsCollection> collections) {
        if (collections == null) {
            return List.of();
        }
        return collections.stream()
                .map(PairMapper::collectionToModel)
                .filter(Objects::nonNull)
                .toList();
    }

    public static PairsCollection modelToCollection(PipelinePairsBatch batch) {
        if (batch == null) {
            return null;
        }
        PairsCollection collection = new PairsCollection();
        collection.setPipelineId(batch.pipelineId());
        List<PairsCollection.Pair> mongoPairs = batch.pairs() == null
                ? List.of()
                : batch.pairs().stream()
                        .filter(Objects::nonNull)
                        .map(PairMapper::modelToPair)
                        .toList();
        collection.setPairs(mongoPairs);
        return collection;
    }

    private static TermPair pairToModel(PairsCollection.Pair pair) {
        return new TermPair(pair.getFirstTerm(), pair.getSecondTerm());
    }

    private static PairsCollection.Pair modelToPair(TermPair termPair) {
        PairsCollection.Pair pair = new PairsCollection.Pair();
        pair.setFirstTerm(termPair.firstTerm());
        pair.setSecondTerm(termPair.secondTerm());
        return pair;
    }
}

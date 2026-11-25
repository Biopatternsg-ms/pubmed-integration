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

import com.biopatternsg.domain.model.KbEvent;
import com.biopatternsg.infrastructure.mongo.KbEventCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stateless pure mapper between {@link KbEventCollection} and {@link KbEvent}.
 */
public final class KbEventMapper {

    private KbEventMapper() {
    }

    public static KbEvent collectionToModel(KbEventCollection doc) {
        if (doc == null) {
            return null;
        }
        return new KbEvent(
                doc.id != null ? doc.id.toHexString() : null,
                doc.getPipelineId(),
                doc.getFirst(),
                doc.getRelation(),
                doc.getSecond(),
                doc.getPubmedIds() != null ? doc.getPubmedIds() : new ArrayList<>()
        );
    }

    public static List<KbEvent> collectionToModelList(List<KbEventCollection> docs) {
        if (docs == null) {
            return Collections.emptyList();
        }
        return docs.stream()
                .map(KbEventMapper::collectionToModel)
                .toList();
    }
}

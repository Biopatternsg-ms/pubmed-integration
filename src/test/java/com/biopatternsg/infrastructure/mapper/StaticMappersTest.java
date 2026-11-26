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

import com.biopatternsg.domain.model.AlignedResult;
import com.biopatternsg.domain.model.AlignedResultSummary;
import com.biopatternsg.domain.model.KbEvent;
import com.biopatternsg.domain.model.KbObject;
import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.ports.out.external_repositories.GenerateKbResult;
import com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase.GenerateKbResponse;
import com.biopatternsg.infrastructure.mongo.AlignedResultCollection;
import com.biopatternsg.infrastructure.mongo.KbEventCollection;
import com.biopatternsg.infrastructure.mongo.KbObjectCollection;
import com.biopatternsg.infrastructure.mongo.PubtatorResultCollection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StaticMappersTest {

    @Test
    void alignedResultMapper_handlesNullAlignedAsAndNullCollection() {
        AlignedResult result = new AlignedResult("pipe-1", List.of("TP53"), List.of("KRAS"), null, List.of("TP53"));
        AlignedResultCollection entity = AlignedResultMapper.modelToCollection(result, null);

        assertThat(entity.getPipelineId()).isEqualTo("pipe-1");
        assertThat(entity.getAlignedAs()).isEmpty();
        assertThat(AlignedResultMapper.collectionToSummary(null)).isNull();

        AlignedResultSummary summary = AlignedResultMapper.collectionToSummary(entity);
        assertThat(summary.aligned()).containsExactly("TP53");
    }

    @Test
    void kbEventMapper_mapsNullAndValidCollections() {
        assertThat(KbEventMapper.collectionToModel(null)).isNull();
        assertThat(KbEventMapper.collectionToModelList(null)).isEmpty();

        KbEventCollection col = new KbEventCollection();
        col.setPipelineId("pipe-1");
        col.setFirst("TP53");
        col.setRelation("binds");
        col.setSecond("MDM2");
        col.setPubmedIds(List.of("123"));

        List<KbEvent> events = KbEventMapper.collectionToModelList(List.of(col));
        assertThat(events).hasSize(1);
        assertThat(events.get(0).first()).isEqualTo("TP53");
    }

    @Test
    void kbObjectMapper_mapsNullAndValidCollections() {
        assertThat(KbObjectMapper.collectionToModel(null)).isNull();
        assertThat(KbObjectMapper.collectionToModelList(null)).isEmpty();

        KbObjectCollection col = new KbObjectCollection();
        col.setName("TP53");
        col.setSynonyms(List.of("p53"));

        List<KbObject> objects = KbObjectMapper.collectionToModelList(List.of(col));
        assertThat(objects).hasSize(1);
        assertThat(objects.get(0).name()).isEqualTo("TP53");
        assertThat(objects.get(0).biotypes()).isEmpty();
    }

    @Test
    void buildKnowledgeBaseMapper_mapsNullAndValidResponse() {
        GenerateKbResult empty = BuildKnowledgeBaseMapper.buildKbToModel("pipe-1", null);
        assertThat(empty.events()).isEmpty();
        assertThat(empty.synonyms()).isEmpty();
        assertThat(empty.biotypes()).isEmpty();

        GenerateKbResponse response = new GenerateKbResponse(
                "pipe-1",
                "111",
                List.of(),
                Map.of("TP53", List.of("p53")),
                null,
                Map.of("TP53", "GENE")
        );
        GenerateKbResult mapped = BuildKnowledgeBaseMapper.buildKbToModel("pipe-1", response);
        assertThat(mapped.events()).isEmpty();
        assertThat(mapped.synonyms()).containsKey("TP53");
        assertThat(mapped.biotypes()).containsEntry("TP53", "GENE");
    }

    @Test
    void pubtatorResultMapper_handlesNullsAndRoundTripsCollection() {
        assertThat(PubtatorResultMapper.pubtatorToModel(null)).isNull();
        assertThat(PubtatorResultMapper.modelToCollection(null)).isNull();
        assertThat(PubtatorResultMapper.collectionToModel(null)).isNull();

        PubtatorResult model = new PubtatorResult("999", "Title", "Text", List.of(), List.of());
        PubtatorResultCollection col = PubtatorResultMapper.modelToCollection(model);
        PubtatorResult back = PubtatorResultMapper.collectionToModel(col);
        assertThat(back.pmid()).isEqualTo("999");
        assertThat(back.title()).isEqualTo("Title");
    }
}

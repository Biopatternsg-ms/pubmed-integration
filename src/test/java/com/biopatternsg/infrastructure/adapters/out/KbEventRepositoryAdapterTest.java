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

import com.biopatternsg.domain.model.KbEvent;
import com.biopatternsg.mongo.KbEventCollection;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KbEventRepositoryAdapter Tests")
class KbEventRepositoryAdapterTest {

    @Mock
    private MongoCollection<KbEventCollection> mockCollection;

    @Test
    void findByPipelineIdAndRestriction_veryRestricted_requiresBothEndpoints() {
        KbEventRepositoryAdapter adapter = spy(new KbEventRepositoryAdapter());

        KbEvent ev1 = new KbEvent("pipe-1", "TP53", "binds", "MDM2", List.of("1"));
        KbEvent ev2 = new KbEvent("pipe-1", "TP53", "activates", "CDKN1A", List.of("2"));
        KbEvent ev3 = new KbEvent("pipe-1", "EGFR", "phosphorylates", "AKT1", List.of("3"));

        doReturn(List.of(ev1, ev2, ev3)).when(adapter).findByPipelineId("pipe-1");

        List<KbEvent> result = adapter.findByPipelineIdAndRestriction(
                "pipe-1",
                "VERY_RESTRICTED",
                List.of("tp53", "mdm2")
        );

        assertEquals(1, result.size());
        assertEquals("TP53", result.get(0).first());
        assertEquals("MDM2", result.get(0).second());
    }

    @Test
    void findByPipelineIdAndRestriction_restricted_requiresAtLeastOneEndpoint() {
        KbEventRepositoryAdapter adapter = spy(new KbEventRepositoryAdapter());

        KbEvent ev1 = new KbEvent("pipe-1", "TP53", "binds", "MDM2", List.of("1"));
        KbEvent ev2 = new KbEvent("pipe-1", "TP53", "activates", "CDKN1A", List.of("2"));
        KbEvent ev3 = new KbEvent("pipe-1", "EGFR", "phosphorylates", "AKT1", List.of("3"));

        doReturn(List.of(ev1, ev2, ev3)).when(adapter).findByPipelineId("pipe-1");

        List<KbEvent> result = adapter.findByPipelineIdAndRestriction(
                "pipe-1",
                "RESTRICTED",
                List.of("tp53")
        );

        assertEquals(2, result.size());
        assertTrue(result.contains(ev1));
        assertTrue(result.contains(ev2));
        assertFalse(result.contains(ev3));
    }

    @Test
    void findByPipelineIdAndRestriction_unrestricted_returnsAll() {
        KbEventRepositoryAdapter adapter = spy(new KbEventRepositoryAdapter());

        KbEvent ev1 = new KbEvent("pipe-1", "TP53", "binds", "MDM2", List.of("1"));
        KbEvent ev2 = new KbEvent("pipe-1", "EGFR", "phosphorylates", "AKT1", List.of("2"));

        doReturn(List.of(ev1, ev2)).when(adapter).findByPipelineId("pipe-1");

        List<KbEvent> result = adapter.findByPipelineIdAndRestriction(
                "pipe-1",
                "UNRESTRICTED",
                List.of("TP53")
        );

        assertEquals(2, result.size());
    }

    @Test
    void findByPipelineIdAndRestriction_nullOrBlankPipelineId_returnsEmpty() {
        KbEventRepositoryAdapter adapter = new KbEventRepositoryAdapter();

        assertTrue(adapter.findByPipelineIdAndRestriction(null, "RESTRICTED", List.of("TP53")).isEmpty());
        assertTrue(adapter.findByPipelineIdAndRestriction("  ", "RESTRICTED", List.of("TP53")).isEmpty());
    }

    @Test
    void findByPipelineIdAndRestriction_emptyAlignedObjectsForRestricted_returnsEmpty() {
        KbEventRepositoryAdapter adapter = spy(new KbEventRepositoryAdapter());
        KbEvent ev1 = new KbEvent("pipe-1", "TP53", "binds", "MDM2", List.of("1"));
        doReturn(List.of(ev1)).when(adapter).findByPipelineId("pipe-1");

        assertTrue(adapter.findByPipelineIdAndRestriction("pipe-1", "VERY_RESTRICTED", List.of()).isEmpty());
        assertTrue(adapter.findByPipelineIdAndRestriction("pipe-1", "RESTRICTED", null).isEmpty());
    }

    @Test
    @DisplayName("When upsert receives @PREFIJO_SUFIJO in first or second, it should save only the suffix")
    void upsert_withPrefixSuffixPattern_shouldExtractSuffixForFirstAndSecond() {
        KbEventRepositoryAdapter adapter = spy(new KbEventRepositoryAdapter());
        lenient().doReturn(mockCollection).when(adapter).mongoCollection();

        String pipelineId = "pipe-123";
        String first = "@GENE_ABCG8";
        String relation = "negative_correlation";
        String second = "CREB3L3";
        List<String> pubmedIds = List.of("27818935");

        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> updateCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<UpdateOptions> optionsCaptor = ArgumentCaptor.forClass(UpdateOptions.class);

        adapter.upsert(pipelineId, first, relation, second, pubmedIds);

        verify(mockCollection, times(1)).updateOne(
                filterCaptor.capture(),
                updateCaptor.capture(),
                optionsCaptor.capture()
        );

        BsonDocument filterDoc = filterCaptor.getValue().toBsonDocument(
                BsonDocument.class,
                MongoClientSettings.getDefaultCodecRegistry()
        );
        BsonArray andArray = filterDoc.getArray("$and");
        assertThat(andArray).hasSize(4);

        assertThat(andArray.get(0).asDocument().getString("pipelineId").getValue()).isEqualTo(pipelineId);
        // first should be saved as ABCG8 (without @GENE_)
        assertThat(andArray.get(1).asDocument().getString("first").getValue()).isEqualTo("ABCG8");
        assertThat(andArray.get(2).asDocument().getString("relation").getValue()).isEqualTo(relation);
        // second was CREB3L3 (no @), stays CREB3L3
        assertThat(andArray.get(3).asDocument().getString("second").getValue()).isEqualTo("CREB3L3");

        assertThat(optionsCaptor.getValue().isUpsert()).isTrue();
    }

    @Test
    @DisplayName("When upsert receives @PREFIJO_SUFIJO in second, it should extract suffix as well")
    void upsert_withPrefixSuffixPatternInSecond_shouldExtractSuffix() {
        KbEventRepositoryAdapter adapter = spy(new KbEventRepositoryAdapter());
        lenient().doReturn(mockCollection).when(adapter).mongoCollection();

        String pipelineId = "pipe-123";
        String first = "ABCG8";
        String relation = "positive_correlation";
        String second = "@CHEMICAL_CHOLESTEROL";
        List<String> pubmedIds = List.of("24666631");

        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);

        adapter.upsert(pipelineId, first, relation, second, pubmedIds);

        verify(mockCollection, times(1)).updateOne(
                filterCaptor.capture(),
                any(Bson.class),
                any(UpdateOptions.class)
        );

        BsonDocument filterDoc = filterCaptor.getValue().toBsonDocument(
                BsonDocument.class,
                MongoClientSettings.getDefaultCodecRegistry()
        );
        BsonArray andArray = filterDoc.getArray("$and");

        assertThat(andArray.get(1).asDocument().getString("first").getValue()).isEqualTo("ABCG8");
        assertThat(andArray.get(3).asDocument().getString("second").getValue()).isEqualTo("CHOLESTEROL");
    }
}

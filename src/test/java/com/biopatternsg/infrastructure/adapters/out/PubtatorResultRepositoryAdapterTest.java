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
import com.biopatternsg.mongo.PubtatorResultCollection;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import io.quarkus.mongodb.panache.PanacheQuery;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PubtatorResultRepositoryAdapter - MongoDB operations for pubtator_results")
class PubtatorResultRepositoryAdapterTest {

    @Mock
    private MongoCollection<PubtatorResultCollection> mockCollection;

    @Mock
    private PanacheQuery<PubtatorResultCollection> mockQuery;

    private PubtatorResultRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = spy(new PubtatorResultRepositoryAdapter());
        lenient().doReturn(mockCollection).when(adapter).mongoCollection();
    }

    @Test
    @DisplayName("updateKnowledgeBase should do nothing when pmid or knowledgeBase is null/blank")
    void updateKnowledgeBase_withInvalidInputs_shouldDoNothing() {
        var kb = new PubtatorResult.KnowledgeBase(List.of(), List.of());

        adapter.updateKnowledgeBase(null, kb);
        adapter.updateKnowledgeBase("  ", kb);
        adapter.updateKnowledgeBase("12345", null);

        verifyNoInteractions(mockCollection);
    }

    @Test
    @DisplayName("updateKnowledgeBase should execute updateOne with pmid filter and $set knowledgeBase")
    void updateKnowledgeBase_shouldUpdateMongoDocument() {
        String pmid = "12345678";
        var kb = new PubtatorResult.KnowledgeBase(
                List.of(
                        new PubtatorResult.KbEventItem("IFNG", "positive_correlation", "TLR2"),
                        new PubtatorResult.KbEventItem("TLR2", "positive_correlation", "IFNG")
                ),
                List.of(
                        new PubtatorResult.KbObjectItem("IFNG", List.of("IFNG", "IFN-GAMMA"), List.of("PROTEIN"))
                )
        );

        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> updateCaptor = ArgumentCaptor.forClass(Bson.class);

        adapter.updateKnowledgeBase(pmid, kb);

        verify(mockCollection, times(1)).updateOne(filterCaptor.capture(), updateCaptor.capture());

        CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        BsonDocument filterDoc = filterCaptor.getValue().toBsonDocument(
                BsonDocument.class,
                pojoCodecRegistry
        );
        assertThat(filterDoc.getString("pmid").getValue()).isEqualTo(pmid);

        BsonDocument updateDoc = updateCaptor.getValue().toBsonDocument(
                BsonDocument.class,
                pojoCodecRegistry
        );
        assertThat(updateDoc.containsKey("$set")).isTrue();
        BsonDocument setDoc = updateDoc.getDocument("$set");
        assertThat(setDoc.containsKey("knowledgeBase")).isTrue();

        BsonDocument kbDoc = setDoc.getDocument("knowledgeBase");
        BsonArray eventArray = kbDoc.getArray("event");
        assertThat(eventArray).hasSize(2);
        assertThat(eventArray.get(0).asDocument().getString("first").getValue()).isEqualTo("IFNG");
        assertThat(eventArray.get(0).asDocument().getString("relation").getValue()).isEqualTo("positive_correlation");
        assertThat(eventArray.get(0).asDocument().getString("second").getValue()).isEqualTo("TLR2");

        BsonArray objectsArray = kbDoc.getArray("objects");
        assertThat(objectsArray).hasSize(1);
        assertThat(objectsArray.get(0).asDocument().getString("name").getValue()).isEqualTo("IFNG");
        assertThat(objectsArray.get(0).asDocument().getArray("biotypes").get(0).asString().getValue()).isEqualTo("PROTEIN");
    }

    @Test
    @DisplayName("findByPmid should map knowledgeBase from collection to domain model")
    void findByPmid_withKnowledgeBase_shouldMapToDomain() {
        String pmid = "12345678";

        PubtatorResultCollection.KbEventItem eventCol = new PubtatorResultCollection.KbEventItem();
        eventCol.setFirst("IFNG");
        eventCol.setRelation("positive_correlation");
        eventCol.setSecond("TLR2");

        PubtatorResultCollection.KbObjectItem objectCol = new PubtatorResultCollection.KbObjectItem();
        objectCol.setName("IFNG");
        objectCol.setSynonyms(List.of("IFNG", "IFN-GAMMA"));
        objectCol.setBiotypes(List.of("PROTEIN"));

        PubtatorResultCollection.KnowledgeBase kbCol = new PubtatorResultCollection.KnowledgeBase();
        kbCol.setEvent(List.of(eventCol));
        kbCol.setObjects(List.of(objectCol));

        PubtatorResultCollection doc = new PubtatorResultCollection();
        doc.setPmid(pmid);
        doc.setTitle("Test Title");
        doc.setText("Test Text");
        doc.setKnowledgeBase(kbCol);

        doReturn(mockQuery).when(adapter).find("pmid", pmid);
        when(mockQuery.firstResult()).thenReturn(doc);

        PubtatorResult result = adapter.findByPmid(pmid);

        assertThat(result).isNotNull();
        assertThat(result.hasKnowledgeBase()).isTrue();
        assertThat(result.knowledgeBase().event()).containsExactly(
                new PubtatorResult.KbEventItem("IFNG", "positive_correlation", "TLR2")
        );
        assertThat(result.knowledgeBase().objects()).containsExactly(
                new PubtatorResult.KbObjectItem("IFNG", List.of("IFNG", "IFN-GAMMA"), List.of("PROTEIN"))
        );
    }
}

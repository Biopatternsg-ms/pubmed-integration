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

import com.biopatternsg.mongo.KbObjectCollection;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KbObjectRepositoryAdapter - MongoDB Query rules for kb_objects")
class KbObjectRepositoryAdapterTest {

    private static final String PIPELINE_ID = "pipeline-abc";

    @Mock
    private MongoCollection<KbObjectCollection> mockCollection;

    private KbObjectRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = spy(new KbObjectRepositoryAdapter());
        lenient().doReturn(mockCollection).when(adapter).mongoCollection();
    }

    @Test
    @DisplayName("When synonyms map is null or empty, saveKbObjects should do nothing")
    void saveKbObjects_withNullOrEmptyMap_shouldDoNothing() {
        adapter.saveKbObjects(PIPELINE_ID, null);
        adapter.saveKbObjects(PIPELINE_ID, Collections.emptyMap());

        verifyNoInteractions(mockCollection);
    }

    @Test
    @DisplayName("When map contains entry with null or empty list, it should skip it")
    void saveKbObjects_withEmptySynonymsList_shouldSkipEntry() {
        Map<String, List<String>> synonyms = Map.of(
                "BRCA1", Collections.emptyList()
        );

        adapter.saveKbObjects(PIPELINE_ID, synonyms);

        verifyNoInteractions(mockCollection);
    }

    @Test
    @DisplayName("When KB objects are saved without biotypes, it should generate proper MongoDB Filters, Updates, and Upsert options")
    void saveKbObjects_shouldGenerateProperBsonOperations() {
        // Arrange
        String name = "BRCA1";
        List<String> synonymList = List.of("BR1", "RNF53");
        Map<String, List<String>> synonymsMap = Map.of(name, synonymList);

        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> updateCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<UpdateOptions> optionsCaptor = ArgumentCaptor.forClass(UpdateOptions.class);

        // Act
        adapter.saveKbObjects(PIPELINE_ID, synonymsMap);

        // Assert
        verify(mockCollection, times(1)).updateOne(
                filterCaptor.capture(),
                updateCaptor.capture(),
                optionsCaptor.capture()
        );

        BsonDocument filterDoc = filterCaptor.getValue().toBsonDocument(
                BsonDocument.class,
                MongoClientSettings.getDefaultCodecRegistry()
        );
        assertThat(filterDoc.containsKey("$and")).isTrue();
        BsonArray andArray = filterDoc.getArray("$and");
        assertThat(andArray).hasSize(2);

        BsonDocument firstFilter = andArray.get(0).asDocument();
        BsonDocument secondFilter = andArray.get(1).asDocument();

        assertThat(firstFilter.getString("pipelineId").getValue()).isEqualTo(PIPELINE_ID);
        assertThat(secondFilter.getString("name").getValue()).isEqualTo(name);

        BsonDocument updateDoc = updateCaptor.getValue().toBsonDocument(
                BsonDocument.class,
                MongoClientSettings.getDefaultCodecRegistry()
        );
        assertThat(updateDoc.containsKey("$addToSet")).isTrue();
        BsonDocument addToSetDoc = updateDoc.getDocument("$addToSet");
        assertThat(addToSetDoc.containsKey("synonyms")).isTrue();
        BsonDocument synonymsDoc = addToSetDoc.getDocument("synonyms");

        assertThat(synonymsDoc.containsKey("$each")).isTrue();
        BsonArray eachArray = synonymsDoc.getArray("$each");
        assertThat(eachArray).hasSize(2);
        assertThat(eachArray.get(0).asString().getValue()).isEqualTo("BR1");
        assertThat(eachArray.get(1).asString().getValue()).isEqualTo("RNF53");

        UpdateOptions options = optionsCaptor.getValue();
        assertThat(options.isUpsert()).isTrue();
    }

    @Test
    @DisplayName("When synonyms and biotypes are saved, it should combine $addToSet for synonyms and $set for each synonym biotype")
    void saveKbObjects_withBiotype_shouldGenerateAddToSetAndSetBsonOperations() {
        // Arrange
        String name = "CXCR4";
        List<String> synonymList = List.of("CXCR4", "LCR-1", "CD184");
        Map<String, List<String>> synonymsMap = Map.of(name, synonymList);
        Map<String, String> biotypesMap = Map.of(
                "CXCR4", "protein",
                "LCR-1", "protein",
                "CD184", "protein",
                "AMPICILLIN", "ligand"
        );

        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> updateCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<UpdateOptions> optionsCaptor = ArgumentCaptor.forClass(UpdateOptions.class);

        // Act
        adapter.saveKbObjects(PIPELINE_ID, synonymsMap, biotypesMap);

        // Assert — Only CXCR4 (present in synonymsMap) is persisted; AMPICILLIN (only in biotypesMap) is ignored
        verify(mockCollection, times(1)).updateOne(
                filterCaptor.capture(),
                updateCaptor.capture(),
                optionsCaptor.capture()
        );

        BsonDocument updateDoc = updateCaptor.getValue().toBsonDocument(
                BsonDocument.class,
                MongoClientSettings.getDefaultCodecRegistry()
        );
        assertThat(updateDoc.containsKey("$addToSet")).isTrue();
        assertThat(updateDoc.containsKey("$set")).isTrue();
        BsonDocument setDoc = updateDoc.getDocument("$set");
        assertThat(setDoc.getString("biotypes.CXCR4").getValue()).isEqualTo("protein");
        assertThat(setDoc.getString("biotypes.LCR-1").getValue()).isEqualTo("protein");
        assertThat(setDoc.getString("biotypes.CD184").getValue()).isEqualTo("protein");
        assertThat(optionsCaptor.getValue().isUpsert()).isTrue();
    }
}

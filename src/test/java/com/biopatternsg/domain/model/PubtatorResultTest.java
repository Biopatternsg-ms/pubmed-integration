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
package com.biopatternsg.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PubtatorResult - hasEvents() and KnowledgeBase")
class PubtatorResultTest {

    @Test
    @DisplayName("1. hasEvents should return true when events list has at least one element")
    void hasEvents_shouldReturnTrue_whenEventsListIsNotEmpty() {
        var event = new PubtatorResult.PubtatorEvent("type", "role1", "role2");
        var result = new PubtatorResult("pmid", "title", "text", Collections.emptyList(), List.of(event));

        assertThat(result.hasEvents()).isTrue();
    }

    @Test
    @DisplayName("2. hasEvents should return false when events list is empty")
    void hasEvents_shouldReturnFalse_whenEventsListIsEmpty() {
        var result = new PubtatorResult("pmid", "title", "text", Collections.emptyList(), Collections.emptyList());

        assertThat(result.hasEvents()).isFalse();
    }

    @Test
    @DisplayName("3. hasEvents should return false when events list is null")
    void hasEvents_shouldReturnFalse_whenEventsListIsNull() {
        var result = new PubtatorResult("pmid", "title", "text", Collections.emptyList(), null);

        assertThat(result.hasEvents()).isFalse();
    }

    @Test
    @DisplayName("4. hasKnowledgeBase should return false when knowledgeBase is null")
    void hasKnowledgeBase_shouldReturnFalse_whenNull() {
        var result = new PubtatorResult("pmid", "title", "text", Collections.emptyList(), Collections.emptyList());

        assertThat(result.hasKnowledgeBase()).isFalse();
    }

    @Test
    @DisplayName("5. hasKnowledgeBase should return true when knowledgeBase is present")
    void hasKnowledgeBase_shouldReturnTrue_whenPresent() {
        var kb = new PubtatorResult.KnowledgeBase(Collections.emptyList(), Collections.emptyList());
        var result = new PubtatorResult("pmid", "title", "text", Collections.emptyList(), Collections.emptyList(), kb);

        assertThat(result.hasKnowledgeBase()).isTrue();
    }

    @Test
    @DisplayName("6. KnowledgeBase.from should map events and normalize biotypes to uppercase")
    void knowledgeBaseFrom_shouldMapEventsAndNormalizeBiotypesToUppercase() {
        var kbEvents = List.of(
                new KbEvent("pipe-1", "IFNG", "positive_correlation", "TLR2", List.of("12345")),
                new KbEvent("pipe-1", "TLR2", "positive_correlation", "IFNG", List.of("12345"))
        );
        var synonyms = Map.of("IFNG", List.of("IFNG", "IFN-GAMMA"));
        var biotypes = Map.of("IFNG", "protein");

        var kb = PubtatorResult.KnowledgeBase.from(kbEvents, synonyms, biotypes);

        assertThat(kb.event()).containsExactly(
                new PubtatorResult.KbEventItem("IFNG", "positive_correlation", "TLR2"),
                new PubtatorResult.KbEventItem("TLR2", "positive_correlation", "IFNG")
        );
        assertThat(kb.objects()).containsExactly(
                new PubtatorResult.KbObjectItem("IFNG", List.of("IFNG", "IFN-GAMMA"), List.of("PROTEIN"))
        );
    }
}

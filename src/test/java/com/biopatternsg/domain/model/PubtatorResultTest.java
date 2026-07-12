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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PubtatorResult - hasEvents()")
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
}

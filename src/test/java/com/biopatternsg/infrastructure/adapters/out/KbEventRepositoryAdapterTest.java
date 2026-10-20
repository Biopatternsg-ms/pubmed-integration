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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("KbEventRepositoryAdapter - Restriction Filtering Tests")
class KbEventRepositoryAdapterTest {

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
}

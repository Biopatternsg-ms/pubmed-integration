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
package com.biopatternsg.application.services.impl;

import com.biopatternsg.domain.model.AlignedResult;
import com.biopatternsg.domain.model.ExpertObjectConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExpertObjectAlignmentServiceImplTest {

    private final ExpertObjectAlignmentServiceImpl service = new ExpertObjectAlignmentServiceImpl();

    @Test
    void align_computesAlignedNoAlignedAndAlternatives() {
        List<ExpertObjectConfig> expertObjects = List.of(
                new ExpertObjectConfig("P04637", "HGNC:11998", "tp53"),
                new ExpertObjectConfig("P38398", "HGNC:1100", "p53"),
                new ExpertObjectConfig("P01116", "HGNC:6407", "KRAS")
        );

        Map<String, List<String>> synonymsMap = Map.of(
                "TP53", List.of("TP53", "p53", "P53_HUMAN")
        );

        AlignedResult result = service.align("pipe-1", expertObjects, synonymsMap);

        assertThat(result.pipelineId()).isEqualTo("pipe-1");
        assertThat(result.aligned()).containsExactly("TP53");
        assertThat(result.noAligned()).containsExactly("KRAS");
        assertThat(result.alignedAs()).hasSize(1);
        assertThat(result.alignedAs().get(0).expertObjectName()).isEqualTo("P53");
        assertThat(result.alignedAs().get(0).alternativeIds()).containsExactly("TP53");
        assertThat(result.alignedAndAlternatives()).containsExactly("TP53");
    }

    @Test
    void align_nullInputs_returnsEmptyLists() {
        AlignedResult result = service.align("pipe-1", null, null);

        assertThat(result.aligned()).isEmpty();
        assertThat(result.noAligned()).isEmpty();
        assertThat(result.alignedAs()).isEmpty();
        assertThat(result.alignedAndAlternatives()).isEmpty();
    }
}

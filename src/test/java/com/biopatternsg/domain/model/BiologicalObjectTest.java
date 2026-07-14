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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BiologicalObject - allTerms()")
class BiologicalObjectTest {

    @Test
    @DisplayName("1. Basic fields without duplicates → [name, symbol, synonyms...]")
    void basicFieldsWithoutDuplicates() {
        var obj = new BiologicalObject("id", "BRCA1", "BR1", List.of("breast cancer 1"));

        assertThat(obj.allTerms())
                .containsExactly("BRCA1", "BR1", "breast cancer 1");
    }

    @Test
    @DisplayName("2. Name and symbol duplicated in synonyms → they are deduplicated, result has 3 elements")
    void nameAndSymbolDuplicatedInSynonyms_areDeduplicated() {
        var obj = new BiologicalObject("id", "BRCA1", "BR1", List.of("BRCA1", "BR1", "breast cancer 1"));

        assertThat(obj.allTerms())
                .hasSize(3)
                .containsExactly("BRCA1", "BR1", "breast cancer 1");
    }

    @Test
    @DisplayName("3. Null symbol → does not appear in the list")
    void nullSymbol_doesNotAppear() {
        var obj = new BiologicalObject("id", "BRCA1", null, List.of("breast cancer 1"));

        assertThat(obj.allTerms())
                .containsExactly("BRCA1", "breast cancer 1");
    }

    @Test
    @DisplayName("4. Null synonyms → only name and symbol")
    void nullSynonyms_onlyNameAndSymbol() {
        var obj = new BiologicalObject("id", "BRCA1", "BR1", null);

        assertThat(obj.allTerms())
                .containsExactly("BRCA1", "BR1");
    }

    @Test
    @DisplayName("5. Null name, symbol, and synonyms → empty list")
    void allTermsNull_emptyList() {
        var obj = new BiologicalObject("id", null, null, null);

        assertThat(obj.allTerms()).isEmpty();
    }

    @Test
    @DisplayName("6. Synonyms with internal nulls → nulls are filtered out")
    void synonymsWithInternalNulls_areFilteredOut() {
        var obj = new BiologicalObject("id", "BRCA1", "BR1", Arrays.asList("bc1", null, "bc2"));

        assertThat(obj.allTerms())
                .containsExactly("BRCA1", "BR1", "bc1", "bc2")
                .doesNotContainNull();
    }

    @Test
    @DisplayName("7. Guaranteed order: name -> symbol -> synonyms (insertion order)")
    void insertionOrderIsGuaranteed() {
        var obj = new BiologicalObject("id", "ZZZ", "AAA", List.of("MMM", "BBB"));

        assertThat(obj.allTerms())
                .containsExactly("ZZZ", "AAA", "MMM", "BBB");
    }

    @Test
    @DisplayName("8. Case-insensitive duplicates in synonyms → the original name/symbol takes precedence and synonyms are treated case-sensitively")
    void caseInsensitiveDuplicatesInSynonyms() {
        var obj = new BiologicalObject("id", "BRCA1", "BR1", List.of("brca1", "other"));

        assertThat(obj.allTerms())
                .containsExactly("BRCA1", "BR1", "brca1", "other");
     }

    @Test
    @DisplayName("9. allTerms(maxSynonyms) with positive limit → returns up to limit synonyms")
    void allTermsWithLimit_returnsUpToLimitSynonyms() {
        var obj = new BiologicalObject("id", "BRCA1", "BR1", List.of("syn1", "syn2", "syn3"));

        // Name + Symbol are always included, synonyms should be limited to 2
        assertThat(obj.allTerms(2))
                .containsExactly("BRCA1", "BR1", "syn1", "syn2");
    }

    @Test
    @DisplayName("10. allTerms(maxSynonyms) with zero limit → returns only name and symbol")
    void allTermsWithZeroLimit_returnsOnlyNameAndSymbol() {
        var obj = new BiologicalObject("id", "BRCA1", "BR1", List.of("syn1", "syn2"));

        assertThat(obj.allTerms(0))
                .containsExactly("BRCA1", "BR1");
    }
}

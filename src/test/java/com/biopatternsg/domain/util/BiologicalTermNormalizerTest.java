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
package com.biopatternsg.domain.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BiologicalTermNormalizerTest {

    @Test
    void normalize_prefixedTerm_extractsUppercasePrefixAndSuffix() {
        BiologicalTermNormalizer.NormalizedTerm result = BiologicalTermNormalizer.normalize("  @gene_TP53  ");

        assertEquals("GENE", result.biotypePrefix());
        assertEquals("TP53", result.term());
        assertEquals("TP53", BiologicalTermNormalizer.extractSuffix("  @gene_TP53  "));
    }

    @Test
    void normalize_multipleUnderscores_splitsOnlyOnFirstUnderscore() {
        BiologicalTermNormalizer.NormalizedTerm result = BiologicalTermNormalizer.normalize("@GENE_BRCA_1");

        assertEquals("GENE", result.biotypePrefix());
        assertEquals("BRCA_1", result.term());
    }

    @Test
    void normalize_plainTerm_returnsNullPrefixAndTrimmedTerm() {
        BiologicalTermNormalizer.NormalizedTerm result = BiologicalTermNormalizer.normalize("  Insulin  ");

        assertNull(result.biotypePrefix());
        assertEquals("Insulin", result.term());
        assertEquals("Insulin", BiologicalTermNormalizer.extractSuffix("  Insulin  "));
    }

    @Test
    void normalize_nullOrBlank_returnsOriginalValueWithNullPrefix() {
        BiologicalTermNormalizer.NormalizedTerm nullResult = BiologicalTermNormalizer.normalize(null);
        assertNull(nullResult.biotypePrefix());
        assertNull(nullResult.term());
        assertNull(BiologicalTermNormalizer.extractSuffix(null));

        BiologicalTermNormalizer.NormalizedTerm blankResult = BiologicalTermNormalizer.normalize("   ");
        assertNull(blankResult.biotypePrefix());
        assertEquals("   ", blankResult.term());
        assertEquals("   ", BiologicalTermNormalizer.extractSuffix("   "));
    }
}

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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record BiologicalObject(
        String id,
        String name,
        String symbol,
        List<String> synonyms
) {
    /**
     * Returns a deduplicated, ordered list of all terms that identify this biological object:
     * name first, then symbol, then synonyms — excluding nulls and repeated values.
     * Uses LinkedHashSet to guarantee insertion-order uniqueness.
     */
    public List<String> allTerms() {
        var set = new LinkedHashSet<String>();
        if (name != null)     set.add(name);
        if (symbol != null)   set.add(symbol);
        if (synonyms != null) synonyms.stream().filter(Objects::nonNull).forEach(set::add);
        return new ArrayList<>(set);
    }
}

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure, stateless domain utility for normalizing biological identifiers
 * formatted as {@code @PREFIX_SUFFIX} or plain terms.
 */
public final class BiologicalTermNormalizer {

    private static final Pattern PREFIX_SUFFIX_PATTERN = Pattern.compile("^@([^_]+)_(.+)$");

    private BiologicalTermNormalizer() {
    }

    /**
     * Parsed biological term separating an optional uppercase biotype prefix from the clean term suffix.
     */
    public record NormalizedTerm(String biotypePrefix, String term) {
    }

    /**
     * Normalizes a raw biological term, extracting an optional {@code @PREFIX_} biotype prefix
     * and the trimmed term suffix.
     */
    public static NormalizedTerm normalize(String rawTerm) {
        if (rawTerm == null || rawTerm.isBlank()) {
            return new NormalizedTerm(null, rawTerm);
        }
        String trimmed = rawTerm.trim();
        Matcher matcher = PREFIX_SUFFIX_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String prefix = matcher.group(1).trim().toUpperCase();
            String suffix = matcher.group(2).trim();
            String cleanTerm = suffix.isBlank() ? trimmed : suffix;
            return new NormalizedTerm(prefix, cleanTerm);
        }
        return new NormalizedTerm(null, trimmed);
    }

    /**
     * Extracts only the normalized term suffix from a raw biological term.
     */
    public static String extractSuffix(String rawTerm) {
        return normalize(rawTerm).term();
    }
}

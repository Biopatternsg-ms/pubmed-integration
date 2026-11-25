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

import java.util.Set;

/**
 * Classification of knowledge-base event restriction levels applied during retrieval.
 */
public enum RestrictionLevel {
    UNRESTRICTED,
    RESTRICTED,
    VERY_RESTRICTED;

    /**
     * Parses an external restriction level string into a {@link RestrictionLevel}.
     * Null, blank, {@code "NO_RESTRICTED"}, or unrecognized values default to {@link #UNRESTRICTED}.
     */
    public static RestrictionLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNRESTRICTED;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "RESTRICTED" -> RESTRICTED;
            case "VERY_RESTRICTED" -> VERY_RESTRICTED;
            default -> UNRESTRICTED;
        };
    }

    /**
     * Evaluates whether the given {@link KbEvent} satisfies this restriction level
     * against the normalized uppercase set of aligned object symbols.
     */
    public boolean matches(KbEvent event, Set<String> alignedSet) {
        return switch (this) {
            case UNRESTRICTED -> true;
            case VERY_RESTRICTED -> matchesAligned(event.first(), alignedSet)
                    && matchesAligned(event.second(), alignedSet);
            case RESTRICTED -> matchesAligned(event.first(), alignedSet)
                    || matchesAligned(event.second(), alignedSet);
        };
    }

    private static boolean matchesAligned(String endpoint, Set<String> alignedSet) {
        return endpoint != null && alignedSet.contains(endpoint.trim().toUpperCase());
    }
}

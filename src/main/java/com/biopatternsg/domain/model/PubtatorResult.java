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

import java.util.List;

public record PubtatorResult(
        String pmid,
        String title,
        String text,
        List<PubtatorObject> objects,
        List<PubtatorEvent> events
) {
    public boolean hasEvents() {
        return events != null && !events.isEmpty();
    }

    public record PubtatorObject(
            String identifier,
            String accession,
            String name,
            String normalizedId,
            String type,
            String biotype,
            String text,
            List<Location> locations
    ) {}

    public record Location(int offset, int length) {}

    public record PubtatorEvent(
            String relationType,
            String role1,
            String role2
    ) {}
}

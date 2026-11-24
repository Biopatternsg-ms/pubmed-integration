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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PubtatorResult(
        String pmid,
        String title,
        String text,
        List<PubtatorObject> objects,
        List<PubtatorEvent> events,
        KnowledgeBase knowledgeBase
) {
    public PubtatorResult(
            String pmid,
            String title,
            String text,
            List<PubtatorObject> objects,
            List<PubtatorEvent> events
    ) {
        this(pmid, title, text, objects, events, null);
    }

    public boolean hasEvents() {
        return events != null && !events.isEmpty();
    }

    public boolean hasKnowledgeBase() {
        return knowledgeBase != null;
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

    public record KnowledgeBase(
            List<KbEventItem> event,
            List<KbObjectItem> objects
    ) {
        public static KnowledgeBase from(
                List<KbEvent> events,
                Map<String, List<String>> synonyms,
                Map<String, String> biotypes
        ) {
            List<KbEventItem> eventItems = events == null ? Collections.emptyList() :
                    events.stream()
                            .map(e -> new KbEventItem(e.first(), e.relation(), e.second()))
                            .toList();

            List<KbObjectItem> objectItems = buildObjects(synonyms, biotypes);

            return new KnowledgeBase(eventItems, objectItems);
        }

        public static List<KbObjectItem> buildObjects(
                Map<String, List<String>> synonyms,
                Map<String, String> biotypes
        ) {
            if (synonyms == null || synonyms.isEmpty()) {
                return Collections.emptyList();
            }

            List<KbObjectItem> result = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : synonyms.entrySet()) {
                String name = entry.getKey();
                List<String> synonymList = entry.getValue();

                if (synonymList == null || synonymList.isEmpty()) {
                    continue;
                }

                List<String> resolvedBiotypes = resolveBiotypes(name, synonymList, biotypes);
                result.add(new KbObjectItem(
                        name,
                        List.copyOf(synonymList),
                        resolvedBiotypes
                ));
            }
            return Collections.unmodifiableList(result);
        }

        private static List<String> resolveBiotypes(
                String name,
                List<String> synonymList,
                Map<String, String> biotypes
        ) {
            if (biotypes == null || biotypes.isEmpty()) {
                return Collections.emptyList();
            }

            Set<String> uniqueBiotypes = new LinkedHashSet<>();
            String mainBiotype = biotypes.get(name);
            if (mainBiotype != null && !mainBiotype.isBlank()) {
                uniqueBiotypes.add(mainBiotype.trim().toUpperCase());
            }

            for (String syn : synonymList) {
                if (syn == null || syn.isBlank()) {
                    continue;
                }
                String synBiotype = biotypes.getOrDefault(syn, mainBiotype);
                if (synBiotype != null && !synBiotype.isBlank()) {
                    uniqueBiotypes.add(synBiotype.trim().toUpperCase());
                }
            }

            return List.copyOf(uniqueBiotypes);
        }
    }

    public record KbEventItem(
            String first,
            String relation,
            String second
    ) {}

    public record KbObjectItem(
            String name,
            List<String> synonyms,
            List<String> biotypes
    ) {}
}

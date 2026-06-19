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
package com.biopatternsg.infrastructure.mapper;

import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.infrastructure.adapters.dtos.pubtator.PubtatorApiResponse.AnnotationDto;
import com.biopatternsg.infrastructure.adapters.dtos.pubtator.PubtatorApiResponse.LocationDto;
import com.biopatternsg.infrastructure.adapters.dtos.pubtator.PubtatorApiResponse.PassageDto;
import com.biopatternsg.infrastructure.adapters.dtos.pubtator.PubtatorApiResponse.PubtatorDocumentDto;
import com.biopatternsg.infrastructure.adapters.dtos.pubtator.PubtatorApiResponse.RelationDto;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class PubtatorResultMapper {

    public PubtatorResult toDomain(PubtatorDocumentDto doc) {
        String title = "";
        String text = "";
        List<PubtatorResult.PubtatorObject> objects = new ArrayList<>();
        List<PubtatorResult.PubtatorEvent> events = new ArrayList<>();

        if (doc.passages() != null) {
            for (PassageDto passage : doc.passages()) {
                String type = passage.infons() != null && passage.infons().type() != null
                        ? passage.infons().type() : "";

                if ("title".equalsIgnoreCase(type)) {
                    title = passage.text() != null ? passage.text() : "";
                } else if ("abstract".equalsIgnoreCase(type)) {
                    text = passage.text() != null ? passage.text() : "";
                }

                if (passage.annotations() != null) {
                    for (AnnotationDto ann : passage.annotations()) {
                        mapAnnotation(ann, objects);
                    }
                }
            }
        }

        if (doc.relations() != null) {
            for (RelationDto rel : doc.relations()) {
                mapRelation(rel, events);
            }
        }

        return new PubtatorResult(
                doc.id() != null ? doc.id() : "",
                title,
                text,
                objects,
                events
        );
    }

    private void mapAnnotation(AnnotationDto ann, List<PubtatorResult.PubtatorObject> objects) {
        if (ann.infons() == null) return;

        String accession = ann.infons().accession();
        if (accession == null || accession.isEmpty() || "null".equals(accession)) return;

        objects.add(new PubtatorResult.PubtatorObject(
                valueOrDefault(ann.infons().identifier()),
                accession,
                valueOrDefault(ann.infons().name()),
                valueOrDefault(ann.infons().normalizedId()),
                valueOrDefault(ann.infons().type()),
                valueOrDefault(ann.infons().biotype()),
                ann.text() != null ? ann.text() : "",
                mapLocations(ann.locations())
        ));
    }

    private List<PubtatorResult.Location> mapLocations(List<LocationDto> locations) {
        if (locations == null) return Collections.emptyList();

        return locations.stream()
                .map(loc -> new PubtatorResult.Location(loc.offset(), loc.length()))
                .toList();
    }

    private void mapRelation(RelationDto rel, List<PubtatorResult.PubtatorEvent> events) {
        if (rel.infons() == null) return;

        events.add(new PubtatorResult.PubtatorEvent(
                rel.infons().type() != null ? rel.infons().type() : "",
                rel.infons().role1() != null && rel.infons().role1().accession() != null
                        ? rel.infons().role1().accession() : "",
                rel.infons().role2() != null && rel.infons().role2().accession() != null
                        ? rel.infons().role2().accession() : ""
        ));
    }

    private String valueOrDefault(String value) {
        return value != null ? value : "";
    }
}

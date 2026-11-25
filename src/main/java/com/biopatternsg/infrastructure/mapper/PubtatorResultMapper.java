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
import com.biopatternsg.infrastructure.mongo.PubtatorResultCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stateless pure mapper for {@link PubtatorResult}, {@link PubtatorDocumentDto}, and {@link PubtatorResultCollection}.
 */
public final class PubtatorResultMapper {

    private PubtatorResultMapper() {
    }

    public static PubtatorResult pubtatorToModel(PubtatorDocumentDto doc) {
        if (doc == null) {
            return null;
        }
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
                valueOrEmpty(doc.id()),
                title,
                text,
                objects,
                events
        );
    }

    public static PubtatorResultCollection modelToCollection(PubtatorResult result) {
        if (result == null) {
            return null;
        }
        PubtatorResultCollection doc = new PubtatorResultCollection();
        doc.setPmid(result.pmid());
        doc.setTitle(result.title());
        doc.setText(result.text());

        doc.setObjects(result.objects() == null ? Collections.emptyList() : result.objects().stream()
                .map(PubtatorResultMapper::toObjectCollection)
                .toList());

        doc.setEvents(result.events() == null ? Collections.emptyList() : result.events().stream()
                .map(PubtatorResultMapper::toEventCollection)
                .toList());

        if (result.knowledgeBase() != null) {
            doc.setKnowledgeBase(knowledgeBaseToCollection(result.knowledgeBase()));
        }

        return doc;
    }

    public static List<PubtatorResultCollection> modelToCollectionList(List<PubtatorResult> results) {
        if (results == null) {
            return Collections.emptyList();
        }
        return results.stream()
                .map(PubtatorResultMapper::modelToCollection)
                .toList();
    }

    public static PubtatorResult collectionToModel(PubtatorResultCollection doc) {
        if (doc == null) {
            return null;
        }
        List<PubtatorResult.PubtatorObject> objects = doc.getObjects() == null ? Collections.emptyList() : doc.getObjects().stream()
                .map(PubtatorResultMapper::toObjectDomain)
                .toList();

        List<PubtatorResult.PubtatorEvent> events = doc.getEvents() == null ? Collections.emptyList() : doc.getEvents().stream()
                .map(PubtatorResultMapper::toEventDomain)
                .toList();

        PubtatorResult.KnowledgeBase knowledgeBase = doc.getKnowledgeBase() == null
                ? null
                : toKnowledgeBaseDomain(doc.getKnowledgeBase());

        return new PubtatorResult(
                valueOrEmpty(doc.getPmid()),
                valueOrEmpty(doc.getTitle()),
                valueOrEmpty(doc.getText()),
                objects,
                events,
                knowledgeBase
        );
    }

    public static List<PubtatorResult> collectionToModelList(List<PubtatorResultCollection> docs) {
        if (docs == null) {
            return Collections.emptyList();
        }
        return docs.stream()
                .map(PubtatorResultMapper::collectionToModel)
                .toList();
    }

    public static PubtatorResultCollection.KnowledgeBase knowledgeBaseToCollection(PubtatorResult.KnowledgeBase kb) {
        if (kb == null) {
            return null;
        }
        PubtatorResultCollection.KnowledgeBase doc = new PubtatorResultCollection.KnowledgeBase();
        doc.setEvent(kb.event() == null ? Collections.emptyList() : kb.event().stream()
                .map(PubtatorResultMapper::toKbEventItemCollection)
                .toList());
        doc.setObjects(kb.objects() == null ? Collections.emptyList() : kb.objects().stream()
                .map(PubtatorResultMapper::toKbObjectItemCollection)
                .toList());
        return doc;
    }

    private static void mapAnnotation(AnnotationDto ann, List<PubtatorResult.PubtatorObject> objects) {
        if (ann.infons() == null) {
            return;
        }
        String accession = ann.infons().accession();
        if (accession == null || accession.isEmpty() || "null".equals(accession)) {
            return;
        }
        objects.add(new PubtatorResult.PubtatorObject(
                valueOrEmpty(ann.infons().identifier()),
                accession,
                valueOrEmpty(ann.infons().name()),
                valueOrEmpty(ann.infons().normalizedId()),
                valueOrEmpty(ann.infons().type()),
                valueOrEmpty(ann.infons().biotype()),
                valueOrEmpty(ann.text()),
                mapLocations(ann.locations())
        ));
    }

    private static List<PubtatorResult.Location> mapLocations(List<LocationDto> locations) {
        if (locations == null) {
            return Collections.emptyList();
        }
        return locations.stream()
                .map(loc -> new PubtatorResult.Location(loc.offset(), loc.length()))
                .toList();
    }

    private static void mapRelation(RelationDto rel, List<PubtatorResult.PubtatorEvent> events) {
        if (rel.infons() == null) {
            return;
        }
        events.add(new PubtatorResult.PubtatorEvent(
                valueOrEmpty(rel.infons().type()),
                rel.infons().role1() != null ? valueOrEmpty(rel.infons().role1().accession()) : "",
                rel.infons().role2() != null ? valueOrEmpty(rel.infons().role2().accession()) : ""
        ));
    }

    private static PubtatorResultCollection.PubtatorObject toObjectCollection(PubtatorResult.PubtatorObject obj) {
        PubtatorResultCollection.PubtatorObject doc = new PubtatorResultCollection.PubtatorObject();
        doc.setIdentifier(obj.identifier());
        doc.setAccession(obj.accession());
        doc.setName(obj.name());
        doc.setNormalizedId(obj.normalizedId());
        doc.setType(obj.type());
        doc.setBiotype(obj.biotype());
        doc.setText(obj.text());
        doc.setLocations(obj.locations() == null ? Collections.emptyList() : obj.locations().stream()
                .map(PubtatorResultMapper::locationToCollection)
                .toList());
        return doc;
    }

    private static PubtatorResultCollection.Location locationToCollection(PubtatorResult.Location loc) {
        PubtatorResultCollection.Location location = new PubtatorResultCollection.Location();
        location.setOffset(loc.offset());
        location.setLength(loc.length());
        return location;
    }

    private static PubtatorResultCollection.PubtatorEvent toEventCollection(PubtatorResult.PubtatorEvent event) {
        PubtatorResultCollection.PubtatorEvent doc = new PubtatorResultCollection.PubtatorEvent();
        doc.setRelationType(event.relationType());
        doc.setRole1(event.role1());
        doc.setRole2(event.role2());
        return doc;
    }

    private static PubtatorResultCollection.KbEventItem toKbEventItemCollection(PubtatorResult.KbEventItem item) {
        PubtatorResultCollection.KbEventItem doc = new PubtatorResultCollection.KbEventItem();
        doc.setFirst(item.first());
        doc.setRelation(item.relation());
        doc.setSecond(item.second());
        return doc;
    }

    private static PubtatorResultCollection.KbObjectItem toKbObjectItemCollection(PubtatorResult.KbObjectItem item) {
        PubtatorResultCollection.KbObjectItem doc = new PubtatorResultCollection.KbObjectItem();
        doc.setName(item.name());
        doc.setSynonyms(item.synonyms() != null ? item.synonyms() : Collections.emptyList());
        doc.setBiotypes(item.biotypes() != null ? item.biotypes() : Collections.emptyList());
        return doc;
    }

    private static PubtatorResult.PubtatorObject toObjectDomain(PubtatorResultCollection.PubtatorObject obj) {
        List<PubtatorResult.Location> locations = obj.getLocations() == null ? Collections.emptyList() : obj.getLocations().stream()
                .map(loc -> new PubtatorResult.Location(loc.getOffset(), loc.getLength()))
                .toList();

        return new PubtatorResult.PubtatorObject(
                valueOrEmpty(obj.getIdentifier()),
                valueOrEmpty(obj.getAccession()),
                valueOrEmpty(obj.getName()),
                valueOrEmpty(obj.getNormalizedId()),
                valueOrEmpty(obj.getType()),
                valueOrEmpty(obj.getBiotype()),
                valueOrEmpty(obj.getText()),
                locations
        );
    }

    private static PubtatorResult.PubtatorEvent toEventDomain(PubtatorResultCollection.PubtatorEvent event) {
        return new PubtatorResult.PubtatorEvent(
                valueOrEmpty(event.getRelationType()),
                valueOrEmpty(event.getRole1()),
                valueOrEmpty(event.getRole2())
        );
    }

    private static PubtatorResult.KnowledgeBase toKnowledgeBaseDomain(PubtatorResultCollection.KnowledgeBase kb) {
        List<PubtatorResult.KbEventItem> eventItems = kb.getEvent() == null ? Collections.emptyList() : kb.getEvent().stream()
                .map(PubtatorResultMapper::toKbEventItemDomain)
                .toList();

        List<PubtatorResult.KbObjectItem> objectItems = kb.getObjects() == null ? Collections.emptyList() : kb.getObjects().stream()
                .map(PubtatorResultMapper::toKbObjectItemDomain)
                .toList();

        return new PubtatorResult.KnowledgeBase(eventItems, objectItems);
    }

    private static PubtatorResult.KbEventItem toKbEventItemDomain(PubtatorResultCollection.KbEventItem item) {
        return new PubtatorResult.KbEventItem(
                valueOrEmpty(item.getFirst()),
                valueOrEmpty(item.getRelation()),
                valueOrEmpty(item.getSecond())
        );
    }

    private static PubtatorResult.KbObjectItem toKbObjectItemDomain(PubtatorResultCollection.KbObjectItem item) {
        return new PubtatorResult.KbObjectItem(
                valueOrEmpty(item.getName()),
                item.getSynonyms() != null ? item.getSynonyms() : Collections.emptyList(),
                item.getBiotypes() != null ? item.getBiotypes() : Collections.emptyList()
        );
    }

    private static String valueOrEmpty(String value) {
        return value != null ? value : "";
    }
}

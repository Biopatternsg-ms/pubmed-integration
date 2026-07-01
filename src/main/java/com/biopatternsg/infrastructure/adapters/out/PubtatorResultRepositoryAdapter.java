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
package com.biopatternsg.infrastructure.adapters.out;

import com.biopatternsg.domain.model.PubtatorResult;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import com.biopatternsg.mongo.PubtatorResultCollection;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
@ApplicationScoped
public class PubtatorResultRepositoryAdapter implements PubtatorResultRepository, PanacheMongoRepository<PubtatorResultCollection> {

    @Override
    public void save(PubtatorResult result) {
        try {
            persist(toCollection(result));
        } catch (Exception e) {
            if (e.getMessage() == null || !e.getMessage().contains("E11000")) {
                throw new InternalServerError(e);
            }
        }
    }

    @Override
    public void saveAll(List<PubtatorResult> results) {
        try {
            persist(results.stream().map(this::toCollection).toList());
        } catch (Exception e) {
            if (e.getMessage() == null || !e.getMessage().contains("E11000")) {
                throw new InternalServerError(e);
            }
        }
    }

    @Override
    public List<String> findExistingPmids(List<String> pmids) {
        if (pmids == null || pmids.isEmpty()) {
            return Collections.emptyList();
        }
        return list("pmid in ?1", pmids).stream()
                .map(PubtatorResultCollection::getPmid)
                .toList();
    }

    private PubtatorResultCollection toCollection(PubtatorResult result) {
        PubtatorResultCollection doc = new PubtatorResultCollection();
        doc.setPmid(result.pmid());
        doc.setTitle(result.title());
        doc.setText(result.text());

        doc.setObjects(result.objects().stream()
                .map(this::toObjectCollection)
                .toList());

        doc.setEvents(result.events().stream()
                .map(this::toEventCollection)
                .toList());

        return doc;
    }

    private PubtatorResultCollection.PubtatorObject toObjectCollection(PubtatorResult.PubtatorObject obj) {
        PubtatorResultCollection.PubtatorObject doc = new PubtatorResultCollection.PubtatorObject();
        doc.setIdentifier(obj.identifier());
        doc.setAccession(obj.accession());
        doc.setName(obj.name());
        doc.setNormalizedId(obj.normalizedId());
        doc.setType(obj.type());
        doc.setBiotype(obj.biotype());
        doc.setText(obj.text());
        doc.setLocations(obj.locations().stream()
                .map(loc -> {
                    PubtatorResultCollection.Location l = new PubtatorResultCollection.Location();
                    l.setOffset(loc.offset());
                    l.setLength(loc.length());
                    return l;
                })
                .toList());
        return doc;
    }

    private PubtatorResultCollection.PubtatorEvent toEventCollection(PubtatorResult.PubtatorEvent event) {
        PubtatorResultCollection.PubtatorEvent doc = new PubtatorResultCollection.PubtatorEvent();
        doc.setRelationType(event.relationType());
        doc.setRole1(event.role1());
        doc.setRole2(event.role2());
        return doc;
    }

    @Override
    public PubtatorResult findByPmid(String pmid) {
        PubtatorResultCollection doc = find("pmid", pmid).firstResult();
        if (doc == null) {
            return null;
        }
        return toDomain(doc);
    }

    private PubtatorResult toDomain(PubtatorResultCollection doc) {
        List<PubtatorResult.PubtatorObject> objects = doc.getObjects() == null ? Collections.emptyList() : doc.getObjects().stream()
                .map(this::toObjectDomain)
                .toList();

        List<PubtatorResult.PubtatorEvent> events = doc.getEvents() == null ? Collections.emptyList() : doc.getEvents().stream()
                .map(this::toEventDomain)
                .toList();

        return new PubtatorResult(
                doc.getPmid() != null ? doc.getPmid() : "",
                doc.getTitle() != null ? doc.getTitle() : "",
                doc.getText() != null ? doc.getText() : "",
                objects,
                events
        );
    }

    private PubtatorResult.PubtatorObject toObjectDomain(PubtatorResultCollection.PubtatorObject obj) {
        List<PubtatorResult.Location> locations = obj.getLocations() == null ? Collections.emptyList() : obj.getLocations().stream()
                .map(loc -> new PubtatorResult.Location(loc.getOffset(), loc.getLength()))
                .toList();

        return new PubtatorResult.PubtatorObject(
                obj.getIdentifier() != null ? obj.getIdentifier() : "",
                obj.getAccession() != null ? obj.getAccession() : "",
                obj.getName() != null ? obj.getName() : "",
                obj.getNormalizedId() != null ? obj.getNormalizedId() : "",
                obj.getType() != null ? obj.getType() : "",
                obj.getBiotype() != null ? obj.getBiotype() : "",
                obj.getText() != null ? obj.getText() : "",
                locations
        );
    }

    private PubtatorResult.PubtatorEvent toEventDomain(PubtatorResultCollection.PubtatorEvent event) {
        return new PubtatorResult.PubtatorEvent(
                event.getRelationType() != null ? event.getRelationType() : "",
                event.getRole1() != null ? event.getRole1() : "",
                event.getRole2() != null ? event.getRole2() : ""
        );
    }
}

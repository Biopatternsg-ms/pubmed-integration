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
package com.biopatternsg.mongo;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Setter
@Getter
@MongoEntity(collection = "pubtator_results")
public class PubtatorResultCollection extends PanacheMongoEntity {
    private String pmid;
    private String title;
    private String text;
    private List<PubtatorObject> objects;
    private List<PubtatorEvent> events;

    @Setter
    @Getter
    public static class PubtatorObject {
        private String identifier;
        private String accession;
        private String name;
        private String normalizedId;
        private String type;
        private String biotype;
        private String text;
        private List<Location> locations;
    }

    @Setter
    @Getter
    public static class Location {
        private int offset;
        private int length;
    }

    @Setter
    @Getter
    public static class PubtatorEvent {
        private String relationType;
        private String role1;
        private String role2;
    }
}

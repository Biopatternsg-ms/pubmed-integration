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
package com.biopatternsg.infrastructure.adapters.dtos.pubtator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PubtatorApiResponse(
    @JsonProperty("PubTator3") List<PubtatorDocumentDto> pubTator3
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PubtatorDocumentDto(
        String id,
        List<PassageDto> passages,
        List<RelationDto> relations
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PassageDto(
        InfonsDto infons,
        String text,
        List<AnnotationDto> annotations
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InfonsDto(
        String type,
        String identifier,
        String accession,
        String name,
        @JsonProperty("normalized_id") String normalizedId,
        String biotype
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AnnotationDto(
        InfonsDto infons,
        String text,
        List<LocationDto> locations
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocationDto(
        int offset,
        int length
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RelationDto(
        RelationInfonsDto infons
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RelationInfonsDto(
        String type,
        RoleDto role1,
        RoleDto role2
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoleDto(
        String accession
    ) {}
}

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
package com.biopatternsg.infrastructure.clients.dtos.buildknowledgebase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlignedInfo(
        List<String> aligned,

        @JsonProperty("no_aligned")
        List<String> noAligned,

        @JsonProperty("aligned_as")
        List<List<String>> alignedAs,

        @JsonProperty("aligned_objs")
        Map<String, Object> alignedObjs,

        @JsonProperty("no_aligned_objs")
        Map<String, Object> noAlignedObjs
) {}

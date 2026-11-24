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
package com.biopatternsg.infrastructure.adapters.in.consumers;

import com.biopatternsg.infrastructure.adapters.dtos.pubtator.PubtatorApiResponse;
import com.biopatternsg.infrastructure.mapper.PubtatorResultMapper;
import com.biopatternsg.domain.model.PubtatorResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class PubtatorResponseParser {

    private final ObjectMapper objectMapper;
    private final PubtatorResultMapper mapper;

    public List<PubtatorResult> parse(String responseJson) throws IOException {
        if (responseJson == null || responseJson.isBlank()) {
            return Collections.emptyList();
        }

        PubtatorApiResponse response = objectMapper.readValue(responseJson, PubtatorApiResponse.class);

        if (response.pubTator3() == null || response.pubTator3().isEmpty()) {
            return Collections.emptyList();
        }

        return response.pubTator3().stream()
                .map(mapper::toDomain)
                .toList();
    }
}

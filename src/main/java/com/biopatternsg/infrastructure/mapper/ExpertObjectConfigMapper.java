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

import com.biopatternsg.domain.model.ExpertObjectConfig;
import com.biopatternsg.infrastructure.adapters.dtos.ExpertObjectConfigDto;

import java.util.List;
import java.util.Objects;

public final class ExpertObjectConfigMapper {

    private ExpertObjectConfigMapper() {
    }

    public static ExpertObjectConfig requestToModel(ExpertObjectConfigDto dto) {
        if (dto == null) {
            return null;
        }
        return new ExpertObjectConfig(
                dto.uniprotId(),
                dto.hgncId(),
                dto.symbol()
        );
    }

    public static List<ExpertObjectConfig> requestListToModel(List<ExpertObjectConfigDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(ExpertObjectConfigMapper::requestToModel)
                .filter(Objects::nonNull)
                .toList();
    }
}

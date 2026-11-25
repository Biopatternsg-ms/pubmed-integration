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
package com.biopatternsg.infrastructure.clients.dtos;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NcbiESearchDtoTest {

    @Test
    void toString_rendersFieldValuesInsteadOfIdentityHash() {
        NcbiESearchResultDto resultDto = new NcbiESearchResultDto("2", List.of("12345", "67890"));
        NcbiESearchResponse response = new NcbiESearchResponse(resultDto);

        assertThat(resultDto.toString()).contains("count=2", "12345", "67890");
        assertThat(response.toString()).contains("eSearchResult=", "12345");
    }
}

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
package com.biopatternsg.application.usecase;

import com.biopatternsg.domain.model.AlignedResultSummary;
import com.biopatternsg.domain.ports.in.GetAlignedResults;
import com.biopatternsg.domain.ports.out.repositories.AlignedResultRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class GetAlignedResultsUseCase implements GetAlignedResults {

    private final AlignedResultRepository alignedResultRepository;

    @Override
    public Optional<AlignedResultSummary> execute(String pipelineId) {
        log.info("Fetching aligned results for pipelineId=[{}]", pipelineId);
        return alignedResultRepository.findByPipelineId(pipelineId);
    }
}

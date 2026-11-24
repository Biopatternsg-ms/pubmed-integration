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

import com.biopatternsg.domain.model.Publication;
import com.biopatternsg.domain.ports.in.GetPublicationsByPmids;
import com.biopatternsg.domain.ports.out.repositories.PubtatorResultRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
@ApplicationScoped
public class GetPublicationsByPmidsUseCase implements GetPublicationsByPmids {

    private final PubtatorResultRepository pubtatorResultRepository;

    public GetPublicationsByPmidsUseCase(PubtatorResultRepository pubtatorResultRepository) {
        this.pubtatorResultRepository = pubtatorResultRepository;
    }

    @Override
    public List<Publication> execute(List<String> pmids) {
        log.info("Executing GetPublicationsByPmidsUseCase for {} PMIDs", pmids != null ? pmids.size() : 0);
        if (pmids == null || pmids.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter and deduplicate PMIDs
        List<String> distinctPmids = pmids.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (distinctPmids.isEmpty()) {
            return Collections.emptyList();
        }

        var results = pubtatorResultRepository.findByPmids(distinctPmids);

        return results.stream()
                .map(res -> new Publication(
                        res.pmid(),
                        res.title(),
                        res.text()
                ))
                .toList();
    }
}

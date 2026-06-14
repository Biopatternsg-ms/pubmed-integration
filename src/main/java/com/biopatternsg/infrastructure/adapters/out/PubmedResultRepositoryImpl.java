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

import com.biopatternsg.domain.ports.out.repositories.PubmedResultRepository;
import com.biopatternsg.mongo.PubmedResultCollection;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class PubmedResultRepositoryImpl implements PubmedResultRepository, PanacheMongoRepository<PubmedResultCollection> {

    @Override
    public void save(String pipelineId, String pubmedId) {
        try {
            PubmedResultCollection doc = new PubmedResultCollection();
            doc.setPipelineId(pipelineId);
            doc.setPubmedId(pubmedId);
            persist(doc);
        } catch (Exception e) {
            if (e.getMessage() == null || !e.getMessage().contains("E11000")) {
                throw new InternalServerError(e);
            }
        }
    }
}

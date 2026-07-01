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
package com.biopatternsg.infrastructure.adapters.out.producers;

import com.biopatternsg.domain.model.PubtatorSearchRequest;
import com.biopatternsg.domain.ports.out.producers.PubtatorQueueSender;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

@Slf4j
@ApplicationScoped
public class PubtatorQueueSenderRabbitAdapter implements PubtatorQueueSender {

    @Inject
    @Channel("pubtator-out")
    @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)
    Emitter<PubtatorSearchRequest> emitter;

    @Override
    public void send(PubtatorSearchRequest request) {
        try {
            emitter.send(request);
        } catch (Exception e) {
            log.error("Failed to send PubTator message to RabbitMQ: pipelineId=[{}], batchIndex=[{}]", request.pipelineId(), request.batchIndex(), e);
            throw new InternalServerError(e);
        }
    }
}

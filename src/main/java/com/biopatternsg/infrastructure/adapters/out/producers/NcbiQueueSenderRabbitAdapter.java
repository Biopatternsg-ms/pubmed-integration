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

import com.biopatternsg.domain.model.NcbiSearchRequest;
import com.biopatternsg.domain.ports.out.producers.NcbiQueueSender;
import jakarta.enterprise.context.ApplicationScoped;
import com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

@Slf4j
@ApplicationScoped
public class NcbiQueueSenderRabbitAdapter implements NcbiQueueSender {

    @Inject
    @Channel("ncbi-out")
    @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)
    Emitter<NcbiSearchRequest> emitter;

    @Override
    public void send(NcbiSearchRequest request) {
        try {
            emitter.send(request);
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ: term=[{}]", request.term(), e);
            throw new InternalServerError(e);
        }
    }
}

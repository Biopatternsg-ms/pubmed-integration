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

import com.biopatternsg.domain.model.NcbiSearchRequest;
import com.biopatternsg.domain.ports.in.ProcessNcbiSearchRequest;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
@RequiredArgsConstructor
public class NcbiRabbitConsumer {

    private final ProcessNcbiSearchRequest processNcbiSearchRequest;

    @Incoming("ncbi-in")
    @Blocking(ordered = false)
    public void consume(JsonObject jsonMsg) {
        NcbiSearchRequest request = jsonMsg.mapTo(NcbiSearchRequest.class);
        processNcbiSearchRequest.execute(request);
    }
}

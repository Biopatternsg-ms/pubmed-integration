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
package com.biopatternsg.infrastructure.session;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@RequestScoped
public class SessionUtils {

    private MultivaluedMap<String, String> context;

    public String getUserId() {
        if (this.context == null) {
            return null;
        }
        List<String> values = this.context.get("x-user-id");
        if (values == null || values.isEmpty()) {
            return null;
        }
        String userId = values.getFirst();
        return (userId == null || userId.isBlank()) ? null : userId;
    }

}

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

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SessionUtilsTest {

    @Test
    void getUserId_whenContextIsNull_returnsNull() {
        SessionUtils sessionUtils = new SessionUtils();
        assertNull(sessionUtils.getUserId());
    }

    @Test
    void getUserId_whenHeaderMissing_returnsNull() {
        SessionUtils sessionUtils = new SessionUtils();
        sessionUtils.setContext(new MultivaluedHashMap<>());
        assertNull(sessionUtils.getUserId());
    }

    @Test
    void getUserId_whenHeaderEmpty_returnsNull() {
        SessionUtils sessionUtils = new SessionUtils();
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("x-user-id", "   ");
        sessionUtils.setContext(headers);
        assertNull(sessionUtils.getUserId());
    }

    @Test
    void getUserId_whenHeaderPresent_returnsUserId() {
        SessionUtils sessionUtils = new SessionUtils();
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("x-user-id", "user-123");
        sessionUtils.setContext(headers);
        assertEquals("user-123", sessionUtils.getUserId());
    }
}

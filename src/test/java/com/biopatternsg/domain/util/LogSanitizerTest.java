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
package com.biopatternsg.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogSanitizerTest {

    @Test
    @DisplayName("sanitize should return 'null' string when value is null")
    void shouldReturnNullStringWhenNull() {
        assertEquals("null", LogSanitizer.sanitize((String) null));
        assertEquals("null", LogSanitizer.sanitize((Object) null));
    }

    @Test
    @DisplayName("sanitize should return unchanged string when no CRLF present")
    void shouldReturnUnchangedStringWhenClean() {
        assertEquals("pipe-12345", LogSanitizer.sanitize("pipe-12345"));
        assertEquals("CYP7A1", LogSanitizer.sanitize("CYP7A1"));
    }

    @Test
    @DisplayName("sanitize should replace CRLF characters with underscores")
    void shouldReplaceCrlfWithUnderscore() {
        String inputWithCrlf = "malicious\r\ninput\rwith\nnewlines";
        String expected = "malicious__input_with_newlines";
        assertEquals(expected, LogSanitizer.sanitize(inputWithCrlf));
    }

    @Test
    @DisplayName("sanitize with Object parameter converts to string and removes CRLF")
    void shouldSanitizeObjectCorrectly() {
        Integer number = 42;
        assertEquals("42", LogSanitizer.sanitize(number));
    }
}

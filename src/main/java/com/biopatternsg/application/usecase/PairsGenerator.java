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

public class PairsGenerator {
    static class TermsPair {
        String first;
        String second;

        public TermsPair(String first, String second) {
            if (first.compareTo(second) <= 0) {
                this.first = first;
                this.second = second;
            } else {
                this.first = second;
                this.second = first;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TermsPair termsPair = (TermsPair) o;
            // Solo comparamos los términos ya ordenados
            return first.equals(termsPair.first) && second.equals(termsPair.second);
        }

        @Override
        public int hashCode() {
            // El 31 es un primo impar que ayuda a distribuir los hashes y permite optimización por desplazamiento de bits
            return first.hashCode() * 31 + second.hashCode();
        }

        @Override
        public String toString() {
            return "[" + first + ", " + second + "]";
        }
    }
}

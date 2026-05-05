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

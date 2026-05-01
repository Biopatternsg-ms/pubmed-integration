package com.biopatternsg.application.usecase;

public class PairsGenerator {
    // Clase personalizada para manejar el par [a, b] == [b, a]
    static class TermsPair {
        String termino1;
        String termino2;

        public TermsPair(String t1, String t2) {
            // Ordenar alfabéticamente garantiza que [a, b] y [b, a] generen el mismo objeto lógico
            if (t1.compareTo(t2) <= 0) {
                this.termino1 = t1;
                this.termino2 = t2;
            } else {
                this.termino1 = t2;
                this.termino2 = t1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TermsPair termsPair = (TermsPair) o;
            // Solo comparamos los términos ya ordenados
            return termino1.equals(termsPair.termino1) && termino2.equals(termsPair.termino2);
        }

        @Override
        public int hashCode() {
            // El 31 es un primo impar que ayuda a distribuir los hashes y permite optimización por desplazamiento de bits
            return termino1.hashCode() * 31 + termino2.hashCode();
        }

        @Override
        public String toString() {
            return "[" + termino1 + ", " + termino2 + "]";
        }
    }
}

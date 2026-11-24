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
package archtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationGovernanceTest {

    private static final Path USE_CASES_SRC_DIR = Path.of("src/main/java/com/biopatternsg/application/usecase");
    private static final Path USE_CASES_DOCS_DIR = Path.of(".agents/use_cases");
    private static final Path USE_CASES_README = USE_CASES_DOCS_DIR.resolve("README.md");
    private static final Path ARCHITECTURE_DOC = Path.of(".agents/architecture.md");

    @DisplayName("Every UseCase must have a matching doc with sequenceDiagram, README index, and Section 5 entry, with no orphan docs")
    @Test
    void everyUseCaseIsDocumentedAndIndexedWithoutOrphans() throws IOException {
        List<String> useCaseNames = listUseCaseClassNames();
        assertFalse(useCaseNames.isEmpty(), "Expected at least one *UseCase.java class");

        String readmeContent = Files.readString(USE_CASES_README);
        String section5Content = extractArchitectureSection5(Files.readString(ARCHITECTURE_DOC));

        for (String useCaseName : useCaseNames) {
            String docFileName = useCaseName + ".md";
            Path docFile = USE_CASES_DOCS_DIR.resolve(docFileName);

            assertTrue(Files.exists(docFile), "Missing Use Case documentation: " + docFile);
            String docContent = Files.readString(docFile);
            assertTrue(docContent.contains("sequenceDiagram"),
                    "Use Case documentation must contain a Mermaid sequenceDiagram: " + docFile);
            assertTrue(readmeContent.contains(docFileName),
                    "Use Case documentation must be indexed in .agents/use_cases/README.md: " + docFileName);
            assertTrue(section5Content.contains(docFileName),
                    "Use Case documentation must be indexed in Section 5 of .agents/architecture.md: " + docFileName);
        }

        Set<String> expectedDocs = useCaseNames.stream()
                .map(name -> name + ".md")
                .collect(Collectors.toSet());

        try (Stream<Path> docFiles = Files.list(USE_CASES_DOCS_DIR)) {
            List<String> orphans = docFiles
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".md") && !"README.md".equals(name))
                    .filter(name -> !expectedDocs.contains(name))
                    .toList();
            assertTrue(orphans.isEmpty(), "Orphan Use Case documentation files found: " + orphans);
        }
    }

    private List<String> listUseCaseClassNames() throws IOException {
        try (Stream<Path> files = Files.list(USE_CASES_SRC_DIR)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith("UseCase.java"))
                    .map(name -> name.substring(0, name.length() - ".java".length()))
                    .sorted()
                    .toList();
        }
    }

    private String extractArchitectureSection5(String architectureContent) {
        int start = architectureContent.indexOf("## 5. Casos de Uso Principales");
        assertTrue(start >= 0, "Section 5 heading not found in .agents/architecture.md");
        int end = architectureContent.indexOf("## 6.", start);
        return end > start ? architectureContent.substring(start, end) : architectureContent.substring(start);
    }
}

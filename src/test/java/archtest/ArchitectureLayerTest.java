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

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

public class ArchitectureLayerTest {

    private static final String ROOT = "com.biopatternsg";

    private JavaClasses javaClasses;

    @BeforeEach
    void init() {
        this.javaClasses = new ClassFileImporter().importPackages(ROOT);
    }

    @DisplayName("The class in the layer applications should only used for others class by the same layer")
    @Test
    void layerApplicationTest() {
        Architectures.LayeredArchitecture architecture = layeredArchitecture()
                .consideringAllDependencies()
                .layer("Application").definedBy(ROOT + ".application..")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Application");
        architecture.check(this.javaClasses);

    }

    @DisplayName("The implementation of the uses cases should not invoked directly")
    @Test
    void layerApplicationUsesCasesTest() {

        Architectures.LayeredArchitecture architecture = layeredArchitecture()
                .consideringAllDependencies()
                .layer("ApplicationUsesCases").definedBy(ROOT+".application.usecase..")
                .whereLayer("ApplicationUsesCases").mayNotBeAccessedByAnyLayer();
        architecture.check(this.javaClasses);

    }

}

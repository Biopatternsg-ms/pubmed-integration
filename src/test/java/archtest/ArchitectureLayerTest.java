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

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ArchitectureLayerTest {

    private static final String ROOT = "com.biopatternsg";

    private JavaClasses javaClasses;

    @BeforeEach
    void init() {
        this.javaClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
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
                .layer("ApplicationUsesCases").definedBy(ROOT + ".application.usecase..")
                .whereLayer("ApplicationUsesCases").mayNotBeAccessedByAnyLayer();
        architecture.check(this.javaClasses);
    }

    @DisplayName("The Domain layer should not depend on Application or Infrastructure layers")
    @Test
    void domainLayerIsolationTest() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".application..",
                        ROOT + ".infrastructure.."
                )
                .check(this.javaClasses);
    }

    @DisplayName("The Application layer should not depend on Infrastructure layer")
    @Test
    void applicationLayerIsolationTest() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".application..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".infrastructure..")
                .check(this.javaClasses);
    }

    @DisplayName("Use Cases should follow naming convention")
    @Test
    void useCasesNamingConventionTest() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage("..application.usecase..")
                .should().haveSimpleNameEndingWith("UseCase")
                .check(this.javaClasses);
    }

    @DisplayName("Controllers should follow naming convention")
    @Test
    void controllersNamingConventionTest() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage("..infrastructure.adapters.in.restcontrollers..")
                .should().haveSimpleNameEndingWith("Controller")
                .check(this.javaClasses);
    }

    @DisplayName("Adapters should follow naming convention")
    @Test
    void adaptersNamingConventionTest() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage("..infrastructure.adapters.out..")
                .should().haveSimpleNameEndingWith("Adapter")
                .check(this.javaClasses);
    }

    @DisplayName("Domain layer should not depend on delivery, persistence, serialization, or DI frameworks")
    @Test
    void domainShouldNotDependOnFrameworksTest() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.ws.rs..",
                        "jakarta.enterprise..",
                        "jakarta.inject..",
                        "jakarta.xml.bind..",
                        "com.fasterxml.jackson..",
                        "io.quarkus..",
                        "io.vertx..",
                        "org.bson..",
                        "org.eclipse.microprofile..",
                        "org.w3c.dom..",
                        "com.cifertech.."
                )
                .check(this.javaClasses);
    }

    @DisplayName("Application layer should not depend on delivery or persistence frameworks")
    @Test
    void applicationShouldNotDependOnDeliveryOrPersistenceFrameworksTest() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.ws.rs..",
                        "io.vertx..",
                        "io.quarkus.mongodb..",
                        "org.bson..",
                        "com.cifertech.."
                )
                .check(this.javaClasses);
    }

    @DisplayName("Use Cases should not depend on concrete Application Service implementations")
    @Test
    void useCasesShouldNotDependOnServiceImplementationsTest() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".application.usecase..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".application.services.impl..")
                .check(this.javaClasses);
    }

    @DisplayName("Mappers should be final, non-CDI, and declare only private constructors")
    @Test
    void staticMappersShouldBeStatelessFinalUtilityClassesTest() {
        ArchCondition<JavaClass> haveOnlyPrivateConstructors = new ArchCondition<>("have only private constructors") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaConstructor constructor : javaClass.getConstructors()) {
                    if (!constructor.getModifiers().contains(JavaModifier.PRIVATE)) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass,
                                javaClass.getName() + " declares non-private constructor " + constructor.getFullName()));
                    }
                }
            }
        };

        ArchRuleDefinition.classes()
                .that().resideInAPackage("..infrastructure.mapper..")
                .and().areTopLevelClasses()
                .should().haveModifier(JavaModifier.FINAL)
                .andShould().notBeAnnotatedWith(ApplicationScoped.class)
                .andShould().notBeAnnotatedWith(Singleton.class)
                .andShould().notBeAnnotatedWith(RequestScoped.class)
                .andShould(haveOnlyPrivateConstructors)
                .check(this.javaClasses);
    }

    @DisplayName("Application and Adapter classes should use constructor injection instead of field injection")
    @Test
    void noFieldInjectionInApplicationAndAdaptersTest() {
        ArchRuleDefinition.noFields()
                .that().areDeclaredInClassesThat().resideInAnyPackage(
                        ROOT + ".application..",
                        ROOT + ".infrastructure.adapters.."
                )
                .should().beAnnotatedWith(Inject.class)
                .orShould().beAnnotatedWith(ConfigProperty.class)
                .check(this.javaClasses);
    }

    @DisplayName("Inbound adapters should not depend directly on outbound ports")
    @Test
    void inboundAdaptersShouldNotDependOnOutboundPortsTest() {
        ArchRuleDefinition.noClasses()
                .that().resideInAnyPackage(
                        ROOT + ".infrastructure.adapters.in.restcontrollers..",
                        ROOT + ".infrastructure.adapters.in.consumers.."
                )
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".domain.ports.out..")
                .check(this.javaClasses);
    }
}

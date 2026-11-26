# Research & Technical Decisions: Architecture Governance Hardening

**Feature**: `006-archunit-governance-hardening`
**Date**: 2026-11-25

## 1. Custom ArchUnit Conditions for Mappers and Field Injection

- **Decision**:
  - For `staticMappersShouldBeStatelessFinalUtilityClassesTest`: filter `ArchRuleDefinition.classes().that().resideInAPackage("..infrastructure.mapper..").and().areTopLevelClasses()` (avoiding false positives on nested record types if any), and check `haveModifier(JavaModifier.FINAL)`, `notBeAnnotatedWith(...)` for `@ApplicationScoped`, `@Singleton`, `@RequestScoped`, and a custom `ArchCondition<JavaClass>` verifying all declared constructors are `private` (`constructor.getModifiers().contains(JavaModifier.PRIVATE)`).
  - For `noFieldInjectionInApplicationAndAdaptersTest`: use `ArchRuleDefinition.noFields().that().areDeclaredInClassesThat().resideInAnyPackage(ROOT + ".application..", ROOT + ".infrastructure.adapters..").should().beAnnotatedWith(Inject.class).orShould().beAnnotatedWith(ConfigProperty.class)`.
- **Rationale**: Filtering `.areTopLevelClasses()` on mappers prevents inner/synthetic classes from failing the `final` + `private` constructor check, while `noFields()` directly targets field-level `@Inject` and `@ConfigProperty` without affecting constructor parameters.
- **Alternatives considered**: Scanning source text with regex — brittle compared to ArchUnit's bytecode-level AST checks.

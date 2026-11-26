# Implementation Plan: Architecture Governance Hardening

**Branch**: `feature/06-architecture-governance-archunit-hardening` | **Date**: 2026-11-25 | **Spec**: [spec.md](file:///home/yacson/Proyectos/biopatternsg-ms/services/pubmed-integration/specs/006-archunit-governance-hardening/spec.md)

**Input**: Feature specification from `/specs/006-archunit-governance-hardening/spec.md`

## Summary

Harden `src/test/java/archtest/ArchitectureLayerTest.java` by adding 6 new ArchUnit governance rules enforcing: (1) Domain layer framework isolation, (2) Application layer delivery/persistence framework isolation, (3) Use Case isolation from `application.services.impl..`, (4) stateless `public final` static mapper structure in `infrastructure.mapper..` with only `private` constructors and zero CDI scope annotations, (5) zero `@Inject` / `@ConfigProperty` field injection in `application..` and `infrastructure.adapters..`, and (6) zero direct dependencies from inbound adapters (`restcontrollers..`, `consumers..`) on `domain.ports.out..`. Keep `DocumentationGovernanceTest` active and verify all 13 ArchUnit tests + `DocumentationGovernanceTest` + full test suite pass cleanly.

## Technical Context

**Language/Version**: Java 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw`)
**Primary Dependencies**: ArchUnit (already in `pom.xml`), JUnit 5, Quarkus 3.31.4
**Storage**: N/A (compile-time / bytecode architecture governance tests)
**Testing**: JUnit 5 + ArchUnit (`ArchitectureLayerTest`, `DocumentationGovernanceTest`)
**Target Platform**: Linux JVM microservice (`services/pubmed-integration`)
**Project Type**: Hexagonal backend microservice
**Performance Goals**: Fast bytecode scan in Surefire (<5s for ArchUnit suite)
**Constraints**: Must not modify `pom.xml` or CI pipelines; must handle top-level vs. inner/nested classes accurately
**Scale/Scope**: 1 test class extended (`ArchitectureLayerTest.java` from 7 to 13 tests)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Hexagonal Layer Isolation (NON-NEGOTIABLE)**: PASS. Directly enforces Domain and Application framework isolation and inbound-to-outbound port isolation.
- **II. Use Cases & Application Services Encapsulation**: PASS. Directly enforces Use Case isolation from `application.services.impl..`.
- **III. Zero Business Logic in Inbound & Outbound Adapters**: PASS. Directly prevents inbound controllers/consumers from bypassing use cases to access `domain.ports.out..`.
- **IV. Java 21 Clean Code & Method Discipline**: PASS. Directly enforces constructor injection (forbids `@Inject` and `@ConfigProperty` field injection).
- **V. Stateless Pure Mappers, Naming Conventions & Use Case Documentation**: PASS. Directly enforces `final`, `private` constructors, and zero CDI scope annotations on `infrastructure.mapper..`, alongside `DocumentationGovernanceTest`.

*Post-Design Re-Check*: PASS — All 6 new ArchUnit rules map 1:1 to Principles I–V.

## Project Structure

### Documentation (this feature)

```text
specs/006-archunit-governance-hardening/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── archunit-rules.md
└── tasks.md             # Phase 2 output (/speckit-tasks command)
```

### Source Code (repository root)

```text
src/test/java/archtest/
├── ArchitectureLayerTest.java         # Extend from 7 to 13 ArchUnit rules
└── DocumentationGovernanceTest.java   # Existing Use Case documentation governance check
```

**Structure Decision**: Single Quarkus microservice test suite under `src/test/java/archtest/`.

## Complexity Tracking

> No Constitution violations; this feature automates constitutional enforcement.

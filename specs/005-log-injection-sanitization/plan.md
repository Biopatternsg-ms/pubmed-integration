# Implementation Plan: Log Injection Prevention and Logging Sanitization

**Branch**: `feature/05-log-injection-prevention-and-logging-sanitization` | **Date**: 2026-11-25 | **Spec**: [spec.md](file:///home/yacson/Proyectos/biopatternsg-ms/services/pubmed-integration/specs/005-log-injection-sanitization/spec.md)

**Input**: Feature specification from `/specs/005-log-injection-sanitization/spec.md`

## Summary

Eliminate CWE-117 log injection risks across all use cases, application services, RabbitMQ producers, repository adapters, and external service wrappers by wrapping every untrusted, external, or queue-derived value (and formatted `e.getMessage()` arguments) in `LogSanitizer.sanitize(...)` or `LogSanitizer.sanitizeCollection(...)`; fix the broken unconsumed `Stream` log in `GenerateAlignedObjectsUseCase`; and expand `LogSanitizerTest` to cover collection sanitization.

## Technical Context

**Language/Version**: Java 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw`)
**Primary Dependencies**: Quarkus 3.31.4, SLF4J (`@Slf4j`)
**Storage**: MongoDB via `PanacheMongoRepository` (adapters only log sanitized parameters)
**Testing**: JUnit 5, ArchUnit (`ArchitectureLayerTest`, `DocumentationGovernanceTest`)
**Target Platform**: Linux JVM microservice (`services/pubmed-integration`)
**Project Type**: Hexagonal backend microservice
**Performance Goals**: Negligible string replacement overhead; no new allocations on clean paths beyond existing logging
**Constraints**: Pure domain `LogSanitizer` in `domain.util` with zero framework dependencies; Apache 2.0 license headers; zero changes to REST/RabbitMQ/MongoDB schemas
**Scale/Scope**: 1 domain utility + test (`LogSanitizer`, `LogSanitizerTest`), 1 application service (`PubmedPairsBuilderServiceImpl`), 6 use cases (`GenerateAlignedObjectsUseCase`, `SearchPubmedByPairsUseCase`, `SearchPubtatorByPmidsUseCase`, `GenerateKbForPipelineUseCase`, `ProcessNcbiSearchRequestUseCase`, `ProcessPubtatorBatchUseCase`), 2 RabbitMQ producers (`NcbiQueueSenderRabbitAdapter`, `PubtatorQueueSenderRabbitAdapter`), 2 repository adapters (`KbEventRepositoryAdapter`, `KbObjectRepositoryAdapter`), and 1 external service (`QueryPubtatorImpl`)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Hexagonal Layer Isolation (NON-NEGOTIABLE)**: PASS. `LogSanitizer` resides in `com.biopatternsg.domain.util` with zero framework imports, safely usable by `domain`, `application`, and `infrastructure`.
- **II. Use Cases & Application Services Encapsulation**: PASS. No port contracts or orchestration boundaries change.
- **III. Zero Business Logic in Inbound & Outbound Adapters**: PASS. Adapters only sanitize log arguments.
- **IV. Java 21 Clean Code & Method Discipline**: PASS. Directly enforces Constitution Principle IV's *Log Injection Prevention (CWE-117 / CodeQL)* rule across all layers and fixes the unconsumed stream log in `GenerateAlignedObjectsUseCase`.
- **V. Stateless Pure Mappers, Naming Conventions & Use Case Documentation**: PASS. `LogSanitizer` remains `public final` with private constructor and `public static` pure functions.

*Post-Design Re-Check*: PASS — All Phase 1 designs satisfy Principles I–V.

## Project Structure

### Documentation (this feature)

```text
specs/005-log-injection-sanitization/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── logging-sanitization.md
└── tasks.md             # Phase 2 output (/speckit-tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/biopatternsg/
├── domain/util/
│   └── LogSanitizer.java                                  # Add sanitizeCollection(Collection<?>)
├── application/
│   ├── services/impl/
│   │   └── PubmedPairsBuilderServiceImpl.java             # Sanitize currentBiologicalObject.id()
│   └── usecase/
│       ├── GenerateAlignedObjectsUseCase.java             # Fix stream log -> sanitized symbol list
│       ├── GenerateKbForPipelineUseCase.java              # Sanitize pmid, kbEvent fields, e.getMessage()
│       ├── ProcessNcbiSearchRequestUseCase.java           # Sanitize request, result.term/ids, e.getMessage()
│       ├── ProcessPubtatorBatchUseCase.java               # Sanitize request, pipelineId, e.getMessage()
│       ├── SearchPubmedByPairsUseCase.java                # Sanitize pipelineId, term, e.getMessage()
│       └── SearchPubtatorByPmidsUseCase.java              # Sanitize pipelineId, e.getMessage()
└── infrastructure/
    ├── adapters/out/
    │   ├── KbEventRepositoryAdapter.java                  # Sanitize pubmedIds & e.getMessage()
    │   ├── KbObjectRepositoryAdapter.java                 # Sanitize processedSynonyms, uniqueBiotypes & e.getMessage()
    │   └── producers/
    │       ├── NcbiQueueSenderRabbitAdapter.java          # Sanitize request.term()
    │       └── PubtatorQueueSenderRabbitAdapter.java      # Sanitize request.pipelineId()
    └── external_services/impl/
        └── QueryPubtatorImpl.java                         # Sanitize pmidsParam
```

**Structure Decision**: Single Quarkus microservice following existing Hexagonal Architecture (`domain`, `application`, `infrastructure`).

## Complexity Tracking

> No Constitution violations; directly fulfills Constitution Principle IV (Log Injection Prevention).

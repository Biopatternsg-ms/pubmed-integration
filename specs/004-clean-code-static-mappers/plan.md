# Implementation Plan: Clean Code Lambdas, Constructor Injection, Null Safety, Explicit String Representation, and Stateless Mappers

**Branch**: `feature/04-clean-code-lambdas-di-null-safety-tostring-and-static-mappers` | **Date**: 2026-11-25 | **Spec**: [spec.md](file:///home/yacson/Proyectos/biopatternsg-ms/services/pubmed-integration/specs/004-clean-code-static-mappers/spec.md)

**Input**: Feature specification from `/specs/004-clean-code-static-mappers/spec.md`

## Summary

Standardize all mappers under `com.biopatternsg.infrastructure.mapper` as `public final` stateless utility classes with `private` constructors and `public static` null-safe pure functions (`PubtatorResultMapper`, `AlignedResultMapper`, `KbEventMapper`, `KbObjectMapper`, `BuildKnowledgeBaseMapper`); replace legacy `.collect(Collectors.toList())` with Java 21 `.toList()`; extract multi-line lambdas into single-responsibility private helpers; convert field-injected components (`NcbiQueueSenderRabbitAdapter`, `PubtatorQueueSenderRabbitAdapter`, `SearchPubtatorByPmidsUseCase`) to explicit constructor injection and remove unused annotations/imports; add explicit `toString()` via Java `record` conversion for `NcbiESearchResponse` and `NcbiESearchResultDto`; and add null-safety / pagination bounds guards in `AlignedResultMapper`/`AlignedResultRepositoryAdapter` and `KbObjectRepositoryAdapter`.

## Technical Context

**Language/Version**: Java 21 (compiled and tested with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw`)
**Primary Dependencies**: Quarkus 3.31.4, Lombok, Jackson (`@JsonIgnoreProperties`, `@JsonProperty`), MicroProfile Config / Reactive Messaging (`@Channel`, `Emitter`)
**Storage**: MongoDB via `PanacheMongoRepository` (`com.biopatternsg.infrastructure.mongo.*`)
**Testing**: JUnit 5, Mockito, ArchUnit (`ArchitectureLayerTest`, `DocumentationGovernanceTest`)
**Target Platform**: Linux JVM microservice (`services/pubmed-integration`)
**Project Type**: Hexagonal backend microservice
**Performance Goals**: Zero runtime overhead from stateless static mappers; preserve existing throughput
**Constraints**: Pure static mappers (`public final`, `private` constructor, zero CDI annotations), constructor injection only, single-expression lambdas, explicit `toString()` on all DTOs/models, Apache 2.0 license headers
**Scale/Scope**: 1 refactored static mapper (`PubtatorResultMapper`), 4 new static mappers (`AlignedResultMapper`, `KbEventMapper`, `KbObjectMapper`, `BuildKnowledgeBaseMapper`), 5 refactored adapters, 2 refactored use cases (`GenerateKbForPipelineUseCase`, `SearchPubtatorByPmidsUseCase`), 2 refactored RabbitMQ producers, 2 client DTOs converted to records, unused annotation cleanup, and unit tests for mappers, null-safety, and pagination guards

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Hexagonal Layer Isolation (NON-NEGOTIABLE)**: PASS. All mappers remain in `infrastructure.mapper.*` bridging `domain.model.*` and `infrastructure.mongo.*` / `infrastructure.clients.dtos.*`.
- **II. Use Cases & Application Services Encapsulation**: PASS. `SearchPubtatorByPmidsUseCase` and `GenerateKbForPipelineUseCase` preserve their Inbound Port contracts while cleaning up DI and multi-line lambdas.
- **III. Zero Business Logic in Inbound & Outbound Adapters**: PASS. Extracting ~150+ lines of inline mapping from outbound adapters into dedicated `infrastructure.mapper.*` classes makes adapters even thinner.
- **IV. Java 21 Clean Code & Method Discipline**: PASS. Replaces `Collectors.toList()` with `.toList()`, extracts multi-line lambdas into private helpers, enforces constructor injection, adds null-safety & pagination guards, and converts `NcbiESearchResponse` / `NcbiESearchResultDto` to Java `record`s (explicit `toString()`).
- **V. Stateless Pure Mappers, Naming Conventions & Use Case Documentation**: PASS. Every class in `infrastructure.mapper.*` becomes `public final` with a `private` constructor, zero CDI annotations, and `public static` null-safe deterministic methods.

*Post-Design Re-Check*: PASS — All Phase 1 designs satisfy Principles I–V.

## Project Structure

### Documentation (this feature)

```text
specs/004-clean-code-static-mappers/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── mappers-and-dtos.md
└── tasks.md             # Phase 2 output (/speckit-tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/biopatternsg/
├── application/
│   ├── services/impl/
│   │   └── BiologicalObjectsServiceImpl.java      # Remove unused @Slf4j
│   └── usecase/
│       ├── GenerateKbForPipelineUseCase.java      # Extract multi-line parallelStream lambda to processPmid(...)
│       └── SearchPubtatorByPmidsUseCase.java      # Constructor-inject @ConfigProperty("pubtator.batch-size")
└── infrastructure/
    ├── adapters/
    │   ├── in/consumers/
    │   │   └── PubtatorResponseParser.java        # Remove PubtatorResultMapper CDI field, call static methods
    │   └── out/
    │       ├── AlignedResultRepositoryAdapter.java    # Delegate to AlignedResultMapper; .toList()
    │       ├── KbEventRepositoryAdapter.java          # Delegate to KbEventMapper; .toList()
    │       ├── KbObjectRepositoryAdapter.java         # Delegate to KbObjectMapper; .toList(); guard page<0 / size<=0
    │       ├── PairRepositoryAdapter.java             # Remove unused @RequiredArgsConstructor
    │       ├── PubmedResultRepositoryAdapter.java     # Remove unused @Slf4j
    │       ├── PubtatorResultRepositoryAdapter.java   # Delegate to PubtatorResultMapper; remove unused @Slf4j
    │       ├── external_repositories/
    │       │   └── BuildKnowledgeBaseAdapter.java     # Delegate to BuildKnowledgeBaseMapper; .toList()
    │       └── producers/
    │           ├── NcbiQueueSenderRabbitAdapter.java      # Constructor-inject @Channel("ncbi-out") Emitter
    │           └── PubtatorQueueSenderRabbitAdapter.java  # Constructor-inject @Channel("pubtator-out") Emitter
    ├── clients/dtos/
    │   ├── NcbiESearchResponse.java               # Convert to Java record (explicit toString())
    │   └── NcbiESearchResultDto.java              # Convert to Java record (explicit toString())
    ├── external_services/impl/
    │   └── QueryNcbiESearchImpl.java              # Update record accessor calls (eSearchResult(), idList())
    ├── mapper/
    │   ├── AlignedResultMapper.java               # NEW static mapper (null-safe alignedAs guard)
    │   ├── BuildKnowledgeBaseMapper.java          # NEW static mapper
    │   ├── KbEventMapper.java                     # NEW static mapper
    │   ├── KbObjectMapper.java                    # NEW static mapper
    │   └── PubtatorResultMapper.java              # REFACTORED to public final static mapper (no @ApplicationScoped)
    └── mongo/
        └── KbObjectCollection.java                # Remove unused java.util.Map import
```

**Structure Decision**: Single Quarkus microservice following existing Hexagonal Architecture (`domain`, `application`, `infrastructure`) under `src/main/java/com/biopatternsg/` and mirrored unit tests under `src/test/java/com/biopatternsg/`.

## Complexity Tracking

> No Constitution violations; all changes reduce complexity and enforce existing clean-code and mapper standards.

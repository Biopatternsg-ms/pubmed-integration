# Implementation Plan: Outbound Adapters Business-Logic Extraction and Parameter Encapsulation

**Branch**: `feature/03-outbound-adapters-business-logic-extraction-and-parameter-encapsulation` | **Date**: 2026-11-25 | **Spec**: [spec.md](file:///home/yacson/Proyectos/biopatternsg-ms/services/pubmed-integration/specs/003-outbound-adapters-param-encapsulation/spec.md)

**Input**: Feature specification from `/specs/003-outbound-adapters-param-encapsulation/spec.md`

## Summary

Extract domain filtering (`RestrictionLevel`) and biological term normalization (`BiologicalTermNormalizer`) out of MongoDB persistence adapters (`KbEventRepositoryAdapter`, `KbObjectRepositoryAdapter`) into the Domain and Application layers; extract heavy algorithmic logic and fix the `HashSet` parallel-stream race condition in `BuildPubmedPairsUseCase` and `GenerateAlignedObjectsUseCase` by introducing `PubmedPairsBuilderService` and `ExpertObjectAlignmentService`; encapsulate all >3-parameter signatures across ports, use cases, services, and adapters into immutable Java `record`s (`BuildPubmedPairsCommand`, `PipelineStepUpdate`, `KbEventKey`, `KbObjectUpsertCommand`, `PubtatorResult`, `KbEvent`); and synchronize affected `.agents/use_cases/*.md` documentation.

## Technical Context

**Language/Version**: Java 21 (compiled and tested via `./mvnw`)
**Primary Dependencies**: Quarkus 3.31.4, Lombok, Jakarta Bean Validation, MicroProfile Config & REST Client, `quarkus-exceptions-handler`, SmallRye Reactive Messaging (RabbitMQ)
**Storage**: MongoDB via `PanacheMongoRepository` (`com.biopatternsg.infrastructure.mongo.*`)
**Testing**: JUnit 5, Mockito, ArchUnit (`ArchitectureLayerTest`, `DocumentationGovernanceTest`)
**Target Platform**: Linux JVM microservice (`services/pubmed-integration`)
**Project Type**: Hexagonal backend microservice
**Performance Goals**: Thread-safe parallel stream pair generation with zero race conditions or lost pairs; preserve existing KB generation and alignment throughput
**Constraints**: Max 3 parameters per method in `domain.ports.*`, `application.usecase.*`, `application.services.*`, `infrastructure.adapters.*`; zero business logic in adapters; zero framework imports in `domain.*`; Apache 2.0 headers; `LogSanitizer.sanitize(...)` on untrusted log inputs
**Scale/Scope**: 6 new domain models/utilities, 2 new application service interfaces + implementations, 5+ refactored adapters/internal services, 4+ updated use cases and callers, 4+ updated use case docs

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Hexagonal Layer Isolation (NON-NEGOTIABLE)**: PASS. `RestrictionLevel`, `BiologicalTermNormalizer`, `BuildPubmedPairsCommand`, `PipelineStepUpdate`, `KbEventKey`, and `KbObjectUpsertCommand` reside in `domain.model.*` / `domain.util.*` with zero framework dependencies. `PubmedPairsBuilderService` and `ExpertObjectAlignmentService` reside in `application.services.*` / `application.services.impl.*` depending only on `domain.*` and `application.services.*`.
- **II. Use Cases & Application Services Encapsulation**: PASS. `BuildPubmedPairsUseCase` and `GenerateAlignedObjectsUseCase` are reduced to lean orchestrators delegating algorithmic logic to `PubmedPairsBuilderService` and `ExpertObjectAlignmentService`. `GetKbEventsByRestrictionUseCase` performs restriction filtering in Application.
- **III. Zero Business Logic in Inbound & Outbound Adapters**: PASS. `KbEventRepositoryAdapter` no longer implements restriction-level branching or regex parsing; `KbObjectRepositoryAdapter` delegates `@PREFIX_SUFFIX` parsing to `BiologicalTermNormalizer` and uses `KbObjectUpsertCommand`.
- **IV. Java 21 Clean Code & Method Discipline**: PASS. All 4–6 parameter signatures are encapsulated into immutable Java `record`s (with automatic `toString()`). `BuildPubmedPairsUseCase` / `PubmedPairsBuilderServiceImpl` replaces `HashSet` mutation inside `parallelStream()` with functional `.flatMap(...).collect(Collectors.toSet())`. Enhanced `switch` over `RestrictionLevel` is used.
- **V. Stateless Pure Mappers, Naming Conventions & Use Case Documentation**: PASS. `BiologicalTermNormalizer` is a `public final` stateless utility with a private constructor. Affected `.agents/use_cases/*.md` files (`BuildPubmedPairsUseCase.md`, `GenerateKbForPipelineUseCase.md`, `GenerateAlignedObjectsUseCase.md`, `GetKbEventsByRestrictionUseCase.md`) will be updated synchronously.

*Post-Design Re-Check*: PASS — All Phase 1 contracts and domain records satisfy Principles I–V with zero exceptions.

## Project Structure

### Documentation (this feature)

```text
specs/003-outbound-adapters-param-encapsulation/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── ports-and-services.md
└── tasks.md             # Phase 2 output (/speckit-tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/biopatternsg/
├── domain/
│   ├── model/
│   │   ├── BuildPubmedPairsCommand.java      # NEW record (1-param BuildPubmedPairs.execute)
│   │   ├── KbEventKey.java                   # NEW record (1-param KbEventRepository.findByRelation)
│   │   ├── KbObjectUpsertCommand.java        # NEW record (2-param KbObjectRepositoryAdapter.upsertKbObject)
│   │   ├── PipelineStepUpdate.java           # NEW record + factory methods (1-param ConfigAndControlRepository.updateStep)
│   │   └── RestrictionLevel.java             # NEW enum + fromString factory
│   ├── ports/
│   │   ├── in/
│   │   │   └── BuildPubmedPairs.java         # UPDATED execute(BuildPubmedPairsCommand)
│   │   └── out/
│   │       ├── external_repositories/
│   │       │   ├── BuildKnowledgeBaseRepoWeb.java   # UPDATED generateKnowledgeBase(String, PubtatorResult)
│   │       │   └── ConfigAndControlRepository.java  # UPDATED updateStep(PipelineStepUpdate)
│   │       └── repositories/
│   │           └── KbEventRepository.java    # UPDATED findByRelation(KbEventKey), upsert(KbEvent); remove findByPipelineIdAndRestriction
│   └── util/
│       └── BiologicalTermNormalizer.java     # NEW stateless utility (@PREFIX_SUFFIX parsing)
├── application/
│   ├── services/
│   │   ├── PubmedPairsBuilderService.java    # NEW interface
│   │   ├── ExpertObjectAlignmentService.java # NEW interface
│   │   └── impl/
│   │       ├── PubmedPairsBuilderServiceImpl.java     # NEW @ApplicationScoped (thread-safe parallelStream)
│   │       └── ExpertObjectAlignmentServiceImpl.java  # NEW @ApplicationScoped (inverted-synonym alignment)
│   └── usecase/
│       ├── BuildPubmedPairsUseCase.java          # UPDATED lean orchestrator
│       ├── GenerateAlignedObjectsUseCase.java    # UPDATED lean orchestrator
│       ├── GenerateKbForPipelineUseCase.java     # UPDATED callers (generateKnowledgeBase, upsert, updateStep)
│       ├── GetKbEventsByRestrictionUseCase.java  # UPDATED restriction filtering via RestrictionLevel
│       ├── ProcessNcbiSearchRequestUseCase.java  # UPDATED updateStep(PipelineStepUpdate)
│       └── ProcessPubtatorBatchUseCase.java      # UPDATED updateStep(PipelineStepUpdate)
└── infrastructure/
    ├── adapters/
    │   ├── in/restcontrollers/
    │   │   └── PubmedController.java         # UPDATED BuildPubmedPairsCommand caller
    │   └── out/
    │       ├── KbEventRepositoryAdapter.java     # UPDATED remove restriction branching & regex
    │       ├── KbObjectRepositoryAdapter.java    # UPDATED use BiologicalTermNormalizer & KbObjectUpsertCommand
    │       ├── ConfigAndControlRepositoryAdapter.java # UPDATED updateStep(PipelineStepUpdate)
    │       └── external_repositories/
    │           └── BuildKnowledgeBaseAdapter.java # UPDATED generateKnowledgeBase(String, PubtatorResult)
    └── internal_services/
        ├── QueryConfigAndControl.java        # UPDATED updateStep(PipelineStepUpdate)
        └── impl/
            └── QueryConfigAndControlImpl.java # UPDATED updateStep(PipelineStepUpdate)
```

**Structure Decision**: Single Quarkus microservice following existing Hexagonal Architecture (`domain`, `application`, `infrastructure`) under `src/main/java/com/biopatternsg/` and mirrored unit tests under `src/test/java/com/biopatternsg/`.

## Complexity Tracking

> No Constitution violations; all changes reduce complexity and enforce existing architectural rules.

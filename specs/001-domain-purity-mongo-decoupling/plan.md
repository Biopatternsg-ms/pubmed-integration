# Implementation Plan: Domain and Application Layer Purity and MongoDB Decoupling

**Branch**: `001-domain-purity-mongo-decoupling` | **Date**: 2026-11-24 | **Spec**: [spec.md](file:///home/yacson/Proyectos/biopatternsg-ms/services/pubmed-integration/specs/001-domain-purity-mongo-decoupling/spec.md)

**Input**: Feature specification from `/specs/001-domain-purity-mongo-decoupling/spec.md`

## Summary

Decouple `com.biopatternsg.domain` and `com.biopatternsg.application` from MongoDB Panache entities, Jackson serialization annotations, and `quarkus-exceptions-handler` HTTP exceptions by relocating all 9 `com.biopatternsg.mongo.*` collections into `com.biopatternsg.infrastructure.mongo.*`, introducing pure domain models (`TermPair`, `PipelinePairsBatch`, `SearchProgress`) and `AlignmentException`, adding stateless static mappers in `com.biopatternsg.infrastructure.mapper`, and introducing `ExpertObjectConfigDto` at the REST boundary.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Quarkus 3.31.4, MongoDB with Panache, SmallRye Reactive Messaging (RabbitMQ), Lombok, Jackson, Jakarta Bean Validation, `quarkus-exceptions-handler`  
**Storage**: MongoDB (`pairs`, `pubmed_results`, `search_progress`, `pubtator_results`, `pubtator_search_progress`, `kb_events`, `kb_objects`, `alignedResults` — schemas and collection names unchanged)  
**Testing**: JUnit 5, Mockito, AssertJ, ArchUnit (`ArchitectureLayerTest`, `DocumentationGovernanceTest`)  
**Target Platform**: Linux server (Quarkus JVM microservice)  
**Project Type**: Hexagonal microservice (`pubmed-integration`)  
**Performance Goals**: Zero runtime overhead beyond lightweight record instantiation and static mapping  
**Constraints**: 100% backward compatibility for MongoDB BSON schemas/collection names and REST JSON wire contracts; strict adherence to Constitution Principles I–V  
**Scale/Scope**: 9 Panache entities relocated, 3 outbound repository ports, 3 repository adapters, 2 rabbit consumers, 3 use cases, 1 REST controller/DTO, 4 static mappers, 5 unit test files, and `.agents/architecture*.md` docs

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Hexagonal Layer Isolation — NON-NEGOTIABLE)**: PASS (Goal of this feature). `domain.*` will depend on zero frameworks/infrastructure; `application.*` will depend only on `domain.*`; `infrastructure.mongo.*` will be isolated inside `infrastructure`.
- **Principle II (Use Cases & Application Services Encapsulation)**: PASS. `BuildPubmedPairsUseCase`, `SearchPubmedByPairsUseCase`, and `GenerateAlignedObjectsUseCase` remain behind Inbound Ports and coordinate Outbound Ports/Application Services.
- **Principle III (Zero Business Logic in Inbound & Outbound Adapters)**: PASS. `PubmedController`, `NcbiRabbitConsumer`, `PubtatorRabbitConsumer`, and repository adapters only perform DTO/entity-to-domain mapping and port delegation.
- **Principle IV (Java 21 Clean Code & Method Discipline)**: PASS. New domain models and DTOs are Java `record`s (automatic `toString()`, immutable, <= 3 params per method). Log calls retain `LogSanitizer.sanitize(...)`.
- **Principle V (Stateless Pure Mappers, Naming Conventions & Use Case Documentation)**: PASS. All new mappers (`PairMapper`, `SearchProgressMapper`, `PubtatorSearchProgressMapper`, `ExpertObjectConfigMapper`) are `public final` utility classes with private constructors, zero CDI annotations, and `public static` null-safe methods. ArchUnit naming conventions (`*UseCase`, `*Controller`, `*Adapter`) are preserved.

*Post-Phase 1 Re-check*: All Phase 1 artifacts (`data-model.md`, `contracts/internal-ports-and-dtos.md`, `quickstart.md`) satisfy Principles I–V with zero violations.

## Project Structure

### Documentation (this feature)

```text
specs/001-domain-purity-mongo-decoupling/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── internal-ports-and-dtos.md
└── tasks.md             # Phase 2 output (created by /speckit-tasks)
```

### Source Code (repository root)

```text
src/main/java/com/biopatternsg/
├── domain/
│   ├── exception/
│   │   └── AlignmentException.java                  # NEW
│   ├── model/
│   │   ├── ExpertObjectConfig.java                  # MODIFIED (remove Jackson)
│   │   ├── PipelinePairsBatch.java                  # NEW
│   │   ├── SearchProgress.java                      # NEW
│   │   └── TermPair.java                            # NEW
│   └── ports/out/repositories/
│       ├── PairRepository.java                      # MODIFIED (pure domain types)
│       ├── PubtatorSearchProgressRepository.java    # MODIFIED (pure domain types)
│       └── SearchProgressRepository.java            # MODIFIED (pure domain types)
├── application/usecase/
│   ├── BuildPubmedPairsUseCase.java                 # MODIFIED (use PipelinePairsBatch/TermPair)
│   ├── GenerateAlignedObjectsUseCase.java           # MODIFIED (throw AlignmentException)
│   └── SearchPubmedByPairsUseCase.java              # MODIFIED (use PipelinePairsBatch/TermPair)
└── infrastructure/
    ├── adapters/
    │   ├── dtos/
    │   │   ├── ExpertObjectConfigDto.java           # NEW (@JsonIgnoreProperties)
    │   │   └── GenerateAlignedObjectsRequest.java   # MODIFIED (use ExpertObjectConfigDto)
    │   ├── in/
    │   │   ├── consumers/
    │   │   │   ├── NcbiRabbitConsumer.java          # MODIFIED (use SearchProgress)
    │   │   │   └── PubtatorRabbitConsumer.java      # MODIFIED (use SearchProgress)
    │   │   └── restcontrollers/
    │   │       └── PubmedController.java            # MODIFIED (map ExpertObjectConfigDto)
    │   └── out/
    │       ├── AlignedResultRepositoryAdapter.java  # MODIFIED (import infrastructure.mongo)
    │       ├── KbEventRepositoryAdapter.java        # MODIFIED (import infrastructure.mongo)
    │       ├── KbObjectRepositoryAdapter.java       # MODIFIED (import infrastructure.mongo)
    │       ├── PairRepositoryAdapter.java           # MODIFIED (map via PairMapper)
    │       ├── PubmedResultRepositoryAdapter.java   # MODIFIED (import infrastructure.mongo)
    │       ├── PubtatorPmidReaderRepositoryAdapter.java # MODIFIED (import infrastructure.mongo)
    │       ├── PubtatorResultRepositoryAdapter.java # MODIFIED (import infrastructure.mongo)
    │       ├── PubtatorSearchProgressRepositoryAdapter.java # MODIFIED (map via PubtatorSearchProgressMapper)
    │       └── SearchProgressRepositoryAdapter.java # MODIFIED (map via SearchProgressMapper)
    ├── mapper/
    │   ├── ExpertObjectConfigMapper.java            # NEW (static pure mapper)
    │   ├── PairMapper.java                          # NEW (static pure mapper)
    │   ├── PubtatorSearchProgressMapper.java        # NEW (static pure mapper)
    │   └── SearchProgressMapper.java                # NEW (static pure mapper)
    └── mongo/                                       # RELOCATED from com.biopatternsg.mongo.*
        ├── AlignedAsEmbedded.java
        ├── AlignedResultCollection.java
        ├── KbEventCollection.java
        ├── KbObjectCollection.java
        ├── PairsCollection.java
        ├── PubmedResultCollection.java
        ├── PubtatorResultCollection.java
        ├── PubtatorSearchProgressCollection.java
        └── SearchProgressCollection.java

src/test/java/com/biopatternsg/
├── application/usecase/
│   ├── BuildPubmedPairsUseCaseTest.java             # MODIFIED
│   └── GenerateAlignedObjectsUseCaseTest.java       # MODIFIED
└── infrastructure/adapters/out/
    ├── KbEventRepositoryAdapterTest.java            # MODIFIED
    ├── KbObjectRepositoryAdapterTest.java           # MODIFIED
    └── PubtatorResultRepositoryAdapterTest.java     # MODIFIED
```

**Structure Decision**: Single Quarkus hexagonal microservice layout preserving `domain`, `application`, and `infrastructure` package boundaries, with all MongoDB Panache entities relocated under `com.biopatternsg.infrastructure.mongo`.

## Complexity Tracking

> No Constitution Check violations; table not required.

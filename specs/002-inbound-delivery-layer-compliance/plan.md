# Implementation Plan: Inbound Consumers, REST Controller and Lifecycle Delivery-Layer Compliance

**Branch**: `002-inbound-delivery-layer-compliance` | **Date**: 2026-11-24 | **Spec**: [spec.md](file:///home/yacson/Proyectos/biopatternsg-ms/services/pubmed-integration/specs/002-inbound-delivery-layer-compliance/spec.md)

**Input**: Feature specification from `/specs/002-inbound-delivery-layer-compliance/spec.md`

## Summary

Extract business orchestration from `NcbiRabbitConsumer` and `PubtatorRabbitConsumer` into two dedicated Inbound Ports and Use Cases (`ProcessNcbiSearchRequestUseCase` and `ProcessPubtatorBatchUseCase`) with `PubtatorSearchRepoWeb` returning parsed `List<PubtatorResult>`, clean up `PubmedController` (rename `buildTreeMesh` → `buildPubmedPairs`, extract `runAsyncTask`, apply declarative Jakarta Bean Validation), harden `SessionUtils.getUserId()` for null-safety, and fix `AppLifecycleBean` (`@Slf4j`, dynamic delay logging, `try-with-resources` on `Response`).

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Quarkus 3.31.4, SmallRye Reactive Messaging (RabbitMQ), Jakarta Bean Validation, JAX-RS, Lombok, SLF4J  
**Storage**: MongoDB via existing repository ports (`PubmedResultRepository`, `PubtatorResultRepository`, `PairRepository`, `SearchProgressRepository`, `PubtatorSearchProgressRepository`, `PubtatorPmidReaderRepository`)  
**Testing**: JUnit 5, Mockito, AssertJ, ArchUnit (`ArchitectureLayerTest`, `DocumentationGovernanceTest`)  
**Target Platform**: Linux server (Quarkus JVM microservice)  
**Project Type**: Hexagonal microservice (`pubmed-integration`)  
**Performance Goals**: Unchanged async/blocking message throughput; safe connection release on startup  
**Constraints**: Zero changes to RabbitMQ exchanges/queues/channels or REST route paths/JSON schemas; strict adherence to Constitution Principles I–V  
**Scale/Scope**: 2 new inbound ports, 2 new use cases (+ unit tests + `.agents/use_cases/*.md` docs), 1 updated outbound port/adapter, 2 refactored consumers, 1 refactored controller, `SessionUtils` (+ unit test), `AppLifecycleBean` (+ updated unit test)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Hexagonal Layer Isolation — NON-NEGOTIABLE)**: PASS. `ProcessNcbiSearchRequestUseCase` and `ProcessPubtatorBatchUseCase` depend only on `domain.*` ports and models; consumers depend only on `domain.ports.in.*`.
- **Principle II (Use Cases & Application Services Encapsulation)**: PASS. Both queue workflows enter through dedicated Inbound Ports implemented by single-responsibility `*UseCase` classes.
- **Principle III (Zero Business Logic in Inbound & Outbound Adapters)**: PASS. `NcbiRabbitConsumer`, `PubtatorRabbitConsumer`, and `PubmedController` become pure delivery adapters; `PubtatorSearchAdapter` encapsulates JSON parsing.
- **Principle IV (Java 21 Clean Code & Method Discipline)**: PASS. Each new Use Case method has `<= 3` parameters (`NcbiSearchRequest` / `PubtatorSearchRequest`), uses `LogSanitizer.sanitize(...)` on untrusted inputs, closes JAX-RS `Response` with `try-with-resources`, and handles null/empty collections safely.
- **Principle V (Stateless Pure Mappers, Naming Conventions & Use Case Documentation)**: PASS. New Use Cases end with `UseCase`, have English docs with Mermaid `sequenceDiagram` in `.agents/use_cases/`, and are indexed in `.agents/use_cases/README.md` and `.agents/architecture.md` Section 5.

*Post-Phase 1 Re-check*: All Phase 1 design artifacts satisfy Principles I–V with zero violations.

## Project Structure

### Documentation (this feature)

```text
specs/002-inbound-delivery-layer-compliance/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── inbound-ports-and-endpoints.md
└── tasks.md             # Created by /speckit-tasks
```

### Source Code (repository root)

```text
src/main/java/com/biopatternsg/
├── domain/ports/
│   ├── in/
│   │   ├── ProcessNcbiSearchRequest.java                # NEW
│   │   └── ProcessPubtatorBatch.java                    # NEW
│   └── out/external_repositories/
│       └── PubtatorSearchRepoWeb.java                   # MODIFIED (returns List<PubtatorResult>)
├── application/usecase/
│   ├── ProcessNcbiSearchRequestUseCase.java             # NEW
│   └── ProcessPubtatorBatchUseCase.java                 # NEW
└── infrastructure/
    ├── adapters/
    │   ├── in/
    │   │   ├── consumers/
    │   │   │   ├── NcbiRabbitConsumer.java              # MODIFIED (delegates to ProcessNcbiSearchRequest)
    │   │   │   └── PubtatorRabbitConsumer.java          # MODIFIED (delegates to ProcessPubtatorBatch)
    │   │   └── restcontrollers/
    │   │       └── PubmedController.java                # MODIFIED (rename, runAsyncTask, @NotBlank/@Valid)
    │   └── out/external_repositories/
    │       └── PubtatorSearchAdapter.java               # MODIFIED (parses response via PubtatorResponseParser)
    ├── boot_functions/
    │   └── AppLifecycleBean.java                        # MODIFIED (@Slf4j, dynamic delay, try-with-resources)
    └── session/
        └── SessionUtils.java                            # MODIFIED (null-safe getUserId)

src/test/java/com/biopatternsg/
├── application/usecase/
│   ├── ProcessNcbiSearchRequestUseCaseTest.java         # NEW
│   └── ProcessPubtatorBatchUseCaseTest.java             # NEW
└── infrastructure/
    ├── boot_functions/
    │   └── AppLifecycleBeanTest.java                    # MODIFIED (verify Response.close())
    └── session/
        └── SessionUtilsTest.java                        # NEW

.agents/
├── architecture.md                                      # MODIFIED (Section 5 index)
└── use_cases/
    ├── README.md                                        # MODIFIED (index new use cases)
    ├── ProcessNcbiSearchRequestUseCase.md               # NEW
    └── ProcessPubtatorBatchUseCase.md                   # NEW
```

**Structure Decision**: Single Quarkus hexagonal microservice layout preserving `domain`, `application`, and `infrastructure` boundaries.

## Complexity Tracking

> No Constitution Check violations; table not required.

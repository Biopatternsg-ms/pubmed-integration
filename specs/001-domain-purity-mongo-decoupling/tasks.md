---

description: "Task list for Domain and Application Layer Purity and MongoDB Decoupling"
---

# Tasks: Domain and Application Layer Purity and MongoDB Decoupling

**Input**: Design documents from `/specs/001-domain-purity-mongo-decoupling/`
**Prerequisites**: `plan.md` (required), `spec.md` (required for user stories), `research.md`, `data-model.md`, `contracts/internal-ports-and-dtos.md`, `quickstart.md`

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., `US1`, `US2`, `US3`, `US4`)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare target directories and verify baseline compilation before refactoring

- [X] T001 Verify baseline build with `./mvnw clean compile` in repository root
- [X] T002 [P] Create target package directory `src/main/java/com/biopatternsg/infrastructure/mongo/`
- [X] T003 [P] Create target package directory `src/main/java/com/biopatternsg/domain/exception/`

---

## Phase 2: User Story 1 - Isolate Persistence Entities Within Infrastructure (Priority: P1) 🎯 MVP

**Goal**: Relocate all 9 MongoDB Panache entities from `com.biopatternsg.mongo.*` into `com.biopatternsg.infrastructure.mongo.*` while preserving all collection names and document schemas, and update all imports so the codebase compiles and passes existing tests.

**Independent Test**: Verify zero classes remain under `src/main/java/com/biopatternsg/mongo/`, all 9 entities reside in `src/main/java/com/biopatternsg/infrastructure/mongo/` with unchanged `@MongoEntity(collection = ...)` values, and `./mvnw test` passes.

### Implementation for User Story 1

- [X] T004 [P] [US1] Move `AlignedAsEmbedded.java` to `src/main/java/com/biopatternsg/infrastructure/mongo/AlignedAsEmbedded.java` and update package to `com.biopatternsg.infrastructure.mongo`
- [X] T005 [P] [US1] Move `AlignedResultCollection.java` to `src/main/java/com/biopatternsg/infrastructure/mongo/AlignedResultCollection.java` (preserve `@MongoEntity(collection = "alignedResults")`)
- [X] T006 [P] [US1] Move `KbEventCollection.java` to `src/main/java/com/biopatternsg/infrastructure/mongo/KbEventCollection.java` (preserve `@MongoEntity(collection = "kb_events")`)
- [X] T007 [P] [US1] Move `KbObjectCollection.java` to `src/main/java/com/biopatternsg/infrastructure/mongo/KbObjectCollection.java` (preserve `@MongoEntity(collection = "kb_objects")`)
- [X] T008 [P] [US1] Move `PairsCollection.java` to `src/main/java/com/biopatternsg/infrastructure/mongo/PairsCollection.java` (preserve `@MongoEntity(collection = "pairs")`)
- [X] T009 [P] [US1] Move `PubmedResultCollection.java` to `src/main/java/com/biopatternsg/infrastructure/mongo/PubmedResultCollection.java` (preserve `@MongoEntity(collection = "pubmed_results")`)
- [X] T010 [P] [US1] Move `PubtatorResultCollection.java` to `src/main/java/com/biopatternsg/infrastructure/mongo/PubtatorResultCollection.java` (preserve `@MongoEntity(collection = "pubtator_results")`)
- [X] T011 [P] [US1] Move `PubtatorSearchProgressCollection.java` to `src/main/java/com/biopatternsg/infrastructure/mongo/PubtatorSearchProgressCollection.java` (preserve `@MongoEntity(collection = "pubtator_search_progress")`)
- [X] T012 [P] [US1] Move `SearchProgressCollection.java` to `src/main/java/com/biopatternsg/infrastructure/mongo/SearchProgressCollection.java` (preserve `@MongoEntity(collection = "search_progress")`)
- [X] T013 [P] [US1] Update imports from `com.biopatternsg.mongo.*` to `com.biopatternsg.infrastructure.mongo.*` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/AlignedResultRepositoryAdapter.java`, `KbEventRepositoryAdapter.java`, `KbObjectRepositoryAdapter.java`, `PubmedResultRepositoryAdapter.java`, `PubtatorPmidReaderRepositoryAdapter.java`, and `PubtatorResultRepositoryAdapter.java`
- [X] T014 [P] [US1] Update imports from `com.biopatternsg.mongo.*` to `com.biopatternsg.infrastructure.mongo.*` in `src/test/java/com/biopatternsg/infrastructure/adapters/out/KbEventRepositoryAdapterTest.java`, `KbObjectRepositoryAdapterTest.java`, and `PubtatorResultRepositoryAdapterTest.java`
- [X] T015 [US1] Temporarily update `com.biopatternsg.mongo.*` imports to `com.biopatternsg.infrastructure.mongo.*` in `PairRepository.java`, `SearchProgressRepository.java`, `PubtatorSearchProgressRepository.java`, `PairRepositoryAdapter.java`, `SearchProgressRepositoryAdapter.java`, `PubtatorSearchProgressRepositoryAdapter.java`, `NcbiRabbitConsumer.java`, `PubtatorRabbitConsumer.java`, `BuildPubmedPairsUseCase.java`, `SearchPubmedByPairsUseCase.java`, and `BuildPubmedPairsUseCaseTest.java` (so US1 compiles and passes tests independently before US2) and remove empty `src/main/java/com/biopatternsg/mongo/` directory

**Checkpoint**: User Story 1 complete — all 9 Panache entities reside in `com.biopatternsg.infrastructure.mongo.*` and `./mvnw test` passes (note: `ArchitectureLayerTest` will pass `domainLayerIsolationTest` and `applicationLayerIsolationTest` once US2 removes the `infrastructure.mongo` references from `domain` and `application`).

---

## Phase 3: User Story 2 - Decouple Domain Ports and Use Cases from Persistence Entities (Priority: P1)

**Goal**: Introduce pure domain models (`TermPair`, `PipelinePairsBatch`, `SearchProgress`), refactor `PairRepository`, `SearchProgressRepository`, `PubtatorSearchProgressRepository`, their adapters, consumers, and use cases to use pure domain types with stateless static mappers.

**Independent Test**: Verify `PairRepository`, `SearchProgressRepository`, `PubtatorSearchProgressRepository`, `BuildPubmedPairsUseCase`, and `SearchPubmedByPairsUseCase` reference zero `infrastructure.mongo.*` types, and `./mvnw test -Dtest=ArchitectureLayerTest,BuildPubmedPairsUseCaseTest` passes.

### Implementation for User Story 2

- [X] T016 [P] [US2] Create pure Java record `TermPair(String firstTerm, String secondTerm)` in `src/main/java/com/biopatternsg/domain/model/TermPair.java`
- [X] T017 [P] [US2] Create pure Java record `PipelinePairsBatch(String pipelineId, List<TermPair> pairs)` (with null-safe compact constructor defaulting `pairs` to `List.of()`) in `src/main/java/com/biopatternsg/domain/model/PipelinePairsBatch.java`
- [X] T018 [P] [US2] Create pure Java record `SearchProgress(String pipelineId, int completedCount, int totalCount, String userId)` with `public boolean isCompleted()` (`completedCount == totalCount`) in `src/main/java/com/biopatternsg/domain/model/SearchProgress.java`
- [X] T019 [P] [US2] Create stateless `public final` static mapper `PairMapper` (`collectionToModel`, `collectionListToModel`, `modelToCollection`) in `src/main/java/com/biopatternsg/infrastructure/mapper/PairMapper.java`
- [X] T020 [P] [US2] Create stateless `public final` static mapper `SearchProgressMapper` (`collectionToModel`) in `src/main/java/com/biopatternsg/infrastructure/mapper/SearchProgressMapper.java`
- [X] T021 [P] [US2] Create stateless `public final` static mapper `PubtatorSearchProgressMapper` (`collectionToModel`) in `src/main/java/com/biopatternsg/infrastructure/mapper/PubtatorSearchProgressMapper.java`
- [X] T022 [P] [US2] Refactor `PairRepository` to `save(PipelinePairsBatch)`, `deleteByPipelineId(String)`, and `List<PipelinePairsBatch> findByPipelineId(String)` in `src/main/java/com/biopatternsg/domain/ports/out/repositories/PairRepository.java`
- [X] T023 [P] [US2] Refactor `SearchProgressRepository` so `incrementAndGet(String pipelineId)` returns `SearchProgress` in `src/main/java/com/biopatternsg/domain/ports/out/repositories/SearchProgressRepository.java`
- [X] T024 [P] [US2] Refactor `PubtatorSearchProgressRepository` so `incrementAndGet(String pipelineId)` returns `SearchProgress` in `src/main/java/com/biopatternsg/domain/ports/out/repositories/PubtatorSearchProgressRepository.java`
- [X] T025 [US2] Update `PairRepositoryAdapter` to map via `PairMapper` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/PairRepositoryAdapter.java` (depends on T019, T022)
- [X] T026 [US2] Update `SearchProgressRepositoryAdapter` to map via `SearchProgressMapper` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/SearchProgressRepositoryAdapter.java` (depends on T020, T023)
- [X] T027 [US2] Update `PubtatorSearchProgressRepositoryAdapter` to map via `PubtatorSearchProgressMapper` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/PubtatorSearchProgressRepositoryAdapter.java` (depends on T021, T024)
- [X] T028 [P] [US2] Refactor `BuildPubmedPairsUseCase` to construct and read `PipelinePairsBatch` and `TermPair` in `src/main/java/com/biopatternsg/application/usecase/BuildPubmedPairsUseCase.java` (depends on T016, T017, T022)
- [X] T029 [P] [US2] Refactor `SearchPubmedByPairsUseCase` to iterate `PipelinePairsBatch` and `TermPair` in `src/main/java/com/biopatternsg/application/usecase/SearchPubmedByPairsUseCase.java` (depends on T016, T017, T022)
- [X] T030 [P] [US2] Update `NcbiRabbitConsumer` to consume `SearchProgress` and `progress.isCompleted()` in `src/main/java/com/biopatternsg/infrastructure/adapters/in/consumers/NcbiRabbitConsumer.java` (depends on T018, T023)
- [X] T031 [P] [US2] Update `PubtatorRabbitConsumer` to consume `SearchProgress` and `progress.isCompleted()` in `src/main/java/com/biopatternsg/infrastructure/adapters/in/consumers/PubtatorRabbitConsumer.java` (depends on T018, T024)
- [X] T032 [US2] Update `BuildPubmedPairsUseCaseTest` to assert against `PipelinePairsBatch` and `TermPair` in `src/test/java/com/biopatternsg/application/usecase/BuildPubmedPairsUseCaseTest.java` (depends on T028)

**Checkpoint**: User Story 2 complete — `domain` and `application` are 100% free of MongoDB/Panache entity references and `ArchitectureLayerTest` passes.

---

## Phase 4: User Story 3 - Decouple Expert Configuration Domain Model from Serialization Annotations (Priority: P2)

**Goal**: Remove `@JsonIgnoreProperties` from `ExpertObjectConfig`, introduce `ExpertObjectConfigDto` at the REST boundary, and map via a stateless static mapper in `PubmedController`.

**Independent Test**: Verify `ExpertObjectConfig.java` has zero `com.fasterxml.jackson.*` imports, `ExpertObjectConfigDto` retains `@JsonIgnoreProperties(ignoreUnknown = true)`, and `./mvnw test` passes.

### Implementation for User Story 3

- [X] T033 [P] [US3] Remove `@JsonIgnoreProperties(ignoreUnknown = true)` and `com.fasterxml.jackson.*` import from `src/main/java/com/biopatternsg/domain/model/ExpertObjectConfig.java`
- [X] T034 [P] [US3] Create `ExpertObjectConfigDto(String uniprotId, String hgncId, String symbol)` with `@JsonIgnoreProperties(ignoreUnknown = true)` in `src/main/java/com/biopatternsg/infrastructure/adapters/dtos/ExpertObjectConfigDto.java`
- [X] T035 [US3] Update `GenerateAlignedObjectsRequest` to use `List<ExpertObjectConfigDto> expertObjects` in `src/main/java/com/biopatternsg/infrastructure/adapters/dtos/GenerateAlignedObjectsRequest.java` (depends on T034)
- [X] T036 [US3] Create stateless `public final` static mapper `ExpertObjectConfigMapper` (`requestToModel`, `requestListToModel`) in `src/main/java/com/biopatternsg/infrastructure/mapper/ExpertObjectConfigMapper.java` (depends on T033, T034)
- [X] T037 [US3] Update `PubmedController.generateAlignedObjects` to map `request.expertObjects()` via `ExpertObjectConfigMapper.requestListToModel(...)` before calling `generateAlignedObjects.execute(...)` in `src/main/java/com/biopatternsg/infrastructure/adapters/in/restcontrollers/PubmedController.java` (depends on T035, T036)

**Checkpoint**: User Story 3 complete — `domain.model.ExpertObjectConfig` is framework-free while `POST /pubmed/generate-aligned-objects` preserves backward-compatible JSON deserialization.

---

## Phase 5: User Story 4 - Decouple Alignment Workflow from HTTP Exception Framework (Priority: P2)

**Goal**: Replace `com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError` in `GenerateAlignedObjectsUseCase` with a pure domain exception `AlignmentException`.

**Independent Test**: Verify zero `com.cifertech.*` imports in `src/main/java/com/biopatternsg/application/` and `GenerateAlignedObjectsUseCaseTest` verifies `AlignmentException` is thrown on failure.

### Implementation for User Story 4

- [X] T038 [P] [US4] Create `AlignmentException` extending `RuntimeException` with `(String message, Throwable cause)` and `(Throwable cause)` constructors in `src/main/java/com/biopatternsg/domain/exception/AlignmentException.java`
- [X] T039 [US4] Replace `InternalServerError` with `AlignmentException` in `src/main/java/com/biopatternsg/application/usecase/GenerateAlignedObjectsUseCase.java` (depends on T038)
- [X] T040 [US4] Update `GenerateAlignedObjectsUseCaseTest` to assert `AlignmentException` instead of `InternalServerError` in `src/test/java/com/biopatternsg/application/usecase/GenerateAlignedObjectsUseCaseTest.java` (depends on T039)

**Checkpoint**: User Story 4 complete — `application` is 100% free of `quarkus-exceptions-handler` HTTP exception dependencies.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Synchronize architecture documentation and run all verification gates from `quickstart.md`

- [X] T041 [P] Update package references from `com.biopatternsg.mongo.*` to `com.biopatternsg.infrastructure.mongo.*` in `.agents/architecture.md` and `.agents/architecture-rules.md`
- [X] T042 Run full validation suite per `quickstart.md`: `./mvnw clean compile`, `./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest`, forbidden-import `grep` checks, and `./mvnw test`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **User Story 1 (Phase 2)**: Depends on Setup (T001–T003). Relocates all 9 MongoDB entities into `infrastructure.mongo`.
- **User Story 2 (Phase 3)**: Depends on User Story 1 (T004–T015) so `infrastructure.mongo.*` types exist at their final package.
- **User Story 3 (Phase 4)**: Depends only on Setup (T001); completely independent of US1 and US2.
- **User Story 4 (Phase 5)**: Depends on Setup (T003); completely independent of US1, US2, and US3.
- **Polish (Phase 6)**: Depends on all User Stories (US1–US4) being complete.

### Parallel Opportunities

- **US1**: T004–T012 (moving 9 entities) and T013–T014 (updating adapter/test imports) can run in parallel.
- **US2**: T016–T024 (domain records, mappers, and port interfaces) can all be created/updated in parallel; T028–T031 (use cases and consumers) can run in parallel once ports/models exist.
- **US3 & US4**: Can be executed completely in parallel with each other and with US2.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (`T001–T003`).
2. Complete Phase 2: User Story 1 (`T004–T015`).
3. **STOP and VALIDATE**: Confirm `com.biopatternsg.mongo` is gone and `./mvnw test` passes.

### Incremental Delivery

1. US1 → Persistence entities isolated in `infrastructure.mongo`.
2. US2 → Domain ports and use cases decoupled via pure domain models + static mappers (`ArchitectureLayerTest` passes).
3. US3 → `ExpertObjectConfig` decoupled from Jackson via `ExpertObjectConfigDto` + `ExpertObjectConfigMapper`.
4. US4 → `GenerateAlignedObjectsUseCase` decoupled from `InternalServerError` via `AlignmentException`.
5. Polish → Docs updated and all Maven/ArchUnit/grep gates verified.

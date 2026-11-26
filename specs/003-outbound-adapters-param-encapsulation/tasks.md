---

description: "Task list for Outbound Adapters Business-Logic Extraction and Parameter Encapsulation"
---

# Tasks: Outbound Adapters Business-Logic Extraction and Parameter Encapsulation

**Input**: Design documents from `/specs/003-outbound-adapters-param-encapsulation/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/ports-and-services.md`, `quickstart.md`

**Tests**: Unit tests are included for all new domain utilities, application services, and refactored use cases/adapters per `quickstart.md` and acceptance criteria.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Baseline Verification)

**Purpose**: Confirm clean baseline compilation and test execution before refactoring

- [X] T001 Verify baseline compilation and tests pass with `./mvnw clean test` in `services/pubmed-integration`

---

## Phase 2: User Story 1 - Extract Domain Filtering and Term Normalization from Persistence Adapters (Priority: P1) 🎯 MVP

**Goal**: Move restriction filtering out of `KbEventRepositoryAdapter` into `GetKbEventsByRestrictionUseCase` using `RestrictionLevel`, and centralize `@PREFIX_SUFFIX` biological term normalization in `BiologicalTermNormalizer`.

**Independent Test**: Run `BiologicalTermNormalizerTest`, `GetKbEventsByRestrictionUseCaseTest`, `KbEventRepositoryAdapterTest`, and `KbObjectRepositoryAdapterTest` without a live MongoDB instance.

### Implementation & Tests for User Story 1

- [X] T002 [P] [US1] Create `RestrictionLevel` enum (`UNRESTRICTED`, `RESTRICTED`, `VERY_RESTRICTED`) with `fromString(String)` factory (`null`, blank, `"NO_RESTRICTED"` → `UNRESTRICTED`) in `src/main/java/com/biopatternsg/domain/model/RestrictionLevel.java`
- [X] T003 [P] [US1] Create stateless `BiologicalTermNormalizer` utility with `NormalizedTerm(String biotypePrefix, String term)` record, `normalize(String)`, and `extractSuffix(String)` encapsulating `^@([^_]+)_(.+)$` in `src/main/java/com/biopatternsg/domain/util/BiologicalTermNormalizer.java`
- [X] T004 [P] [US1] Add unit tests for `@PREFIX_SUFFIX`, multiple underscores, plain terms, and blank/null inputs in `src/test/java/com/biopatternsg/domain/util/BiologicalTermNormalizerTest.java`
- [X] T005 [US1] Move restriction filtering logic (Java 21 enhanced `switch` over `RestrictionLevel`) into `GetKbEventsByRestrictionUseCase` querying `kbEventRepository.findByPipelineId(pipelineId)` in `src/main/java/com/biopatternsg/application/usecase/GetKbEventsByRestrictionUseCase.java`
- [X] T006 [US1] Remove `findByPipelineIdAndRestriction` from `src/main/java/com/biopatternsg/domain/ports/out/repositories/KbEventRepository.java`
- [X] T007 [US1] Remove `PREFIX_SUFFIX_PATTERN`, `extractSuffix`, and `findByPipelineIdAndRestriction` from `KbEventRepositoryAdapter` and delegate term normalization to `BiologicalTermNormalizer.extractSuffix` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/KbEventRepositoryAdapter.java`
- [X] T008 [US1] Remove `PREFIX_SUFFIX_PATTERN` from `KbObjectRepositoryAdapter` and delegate `@PREFIX_SUFFIX` parsing to `BiologicalTermNormalizer.normalize` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/KbObjectRepositoryAdapter.java`
- [X] T009 [P] [US1] Update `GetKbEventsByRestrictionUseCaseTest` to verify `UNRESTRICTED`, `NO_RESTRICTED`, `RESTRICTED`, `VERY_RESTRICTED`, and blank/null restriction filtering via `findByPipelineId` in `src/test/java/com/biopatternsg/application/usecase/GetKbEventsByRestrictionUseCaseTest.java`
- [X] T010 [P] [US1] Update `KbEventRepositoryAdapterTest` (remove `findByPipelineIdAndRestriction` tests, verify normalized persistence) in `src/test/java/com/biopatternsg/infrastructure/adapters/out/KbEventRepositoryAdapterTest.java`

**Checkpoint**: User Story 1 is complete — persistence adapters contain zero restriction branching and zero duplicated regex parsing.

---

## Phase 3: User Story 2 - Encapsulate Complex Pair Generation and Alignment Algorithms with Thread-Safe Concurrency (Priority: P1)

**Goal**: Extract pair-generation logic (fixing the `HashSet` parallel-stream race condition) into `PubmedPairsBuilderService` and expert-object alignment logic into `ExpertObjectAlignmentService`, leaving `BuildPubmedPairsUseCase` and `GenerateAlignedObjectsUseCase` as lean orchestrators.

**Independent Test**: Run `PubmedPairsBuilderServiceImplTest`, `BuildPubmedPairsUseCaseTest`, `ExpertObjectAlignmentServiceImplTest`, and `GenerateAlignedObjectsUseCaseTest`.

### Implementation & Tests for User Story 2

- [X] T011 [P] [US2] Create `PubmedPairsBuilderService` interface in `src/main/java/com/biopatternsg/application/services/PubmedPairsBuilderService.java`
- [X] T012 [P] [US2] Create `ExpertObjectAlignmentService` interface in `src/main/java/com/biopatternsg/application/services/ExpertObjectAlignmentService.java`
- [X] T013 [US2] Implement `@ApplicationScoped` `PubmedPairsBuilderServiceImpl` with level-1 and higher-level pair generation using thread-safe `.flatMap(...).collect(Collectors.toSet())` inside `parallelStream()` in `src/main/java/com/biopatternsg/application/services/impl/PubmedPairsBuilderServiceImpl.java`
- [X] T014 [US2] Implement `@ApplicationScoped` `ExpertObjectAlignmentServiceImpl` encapsulating symbol normalization, inverted-synonym indexing, and alignment calculation in `src/main/java/com/biopatternsg/application/services/impl/ExpertObjectAlignmentServiceImpl.java`
- [X] T015 [US2] Refactor `BuildPubmedPairsUseCase` into a lean orchestrator delegating pair construction to `PubmedPairsBuilderService` in `src/main/java/com/biopatternsg/application/usecase/BuildPubmedPairsUseCase.java`
- [X] T016 [US2] Refactor `GenerateAlignedObjectsUseCase` into a lean orchestrator delegating alignment calculation to `ExpertObjectAlignmentService` and removing 4–5 param private helpers (`saveAlignedResult`, `notifyStatus`) in `src/main/java/com/biopatternsg/application/usecase/GenerateAlignedObjectsUseCase.java`
- [X] T017 [P] [US2] Add unit tests for `PubmedPairsBuilderServiceImpl` (including concurrent parallel-stream execution) in `src/test/java/com/biopatternsg/application/services/impl/PubmedPairsBuilderServiceImplTest.java`
- [X] T018 [P] [US2] Add unit tests for `ExpertObjectAlignmentServiceImpl` in `src/test/java/com/biopatternsg/application/services/impl/ExpertObjectAlignmentServiceImplTest.java`
- [X] T019 [P] [US2] Update `BuildPubmedPairsUseCaseTest` for delegation to `PubmedPairsBuilderService` in `src/test/java/com/biopatternsg/application/usecase/BuildPubmedPairsUseCaseTest.java`
- [X] T020 [P] [US2] Update `GenerateAlignedObjectsUseCaseTest` for delegation to `ExpertObjectAlignmentService` in `src/test/java/com/biopatternsg/application/usecase/GenerateAlignedObjectsUseCaseTest.java`

**Checkpoint**: User Story 2 is complete — pair generation is thread-safe and both use cases are lean orchestrators.

---

## Phase 4: User Story 3 - Encapsulate Multi-Parameter Method Signatures into Cohesive Parameter Objects (Priority: P2)

**Goal**: Ensure zero methods in `domain.ports.*`, `application.usecase.*`, `application.services.*`, or `infrastructure.adapters.*` accept more than 3 individual parameters by introducing `BuildPubmedPairsCommand`, `PipelineStepUpdate`, `KbEventKey`, and `KbObjectUpsertCommand`, and reusing `KbEvent` and `PubtatorResult`.

**Independent Test**: Compile and run all unit tests (`./mvnw test`) verifying all callers and mocks use the new encapsulated parameter records.

### Implementation & Tests for User Story 3

- [X] T021 [P] [US3] Create `BuildPubmedPairsCommand(String pipelineId, boolean useShortName, int levels, String userId)` record in `src/main/java/com/biopatternsg/domain/model/BuildPubmedPairsCommand.java`
- [X] T022 [P] [US3] Create `PipelineStepUpdate(String pipelineId, PipelineSteps step, Status status, String userId, Map<String, String> metrics)` record with `of(...)` and `withMetrics(...)` factories in `src/main/java/com/biopatternsg/domain/model/PipelineStepUpdate.java`
- [X] T023 [P] [US3] Create `KbEventKey(String pipelineId, String first, String relation, String second)` record in `src/main/java/com/biopatternsg/domain/model/KbEventKey.java`
- [X] T024 [P] [US3] Create `KbObjectUpsertCommand(String rawName, List<String> synonyms, Map<String, String> biotypes, List<String> itemBiotypes)` record in `src/main/java/com/biopatternsg/domain/model/KbObjectUpsertCommand.java`
- [X] T025 [US3] Update `BuildPubmedPairs.execute(BuildPubmedPairsCommand)`, `BuildPubmedPairsUseCase`, and `PubmedController` in `src/main/java/com/biopatternsg/domain/ports/in/BuildPubmedPairs.java`, `src/main/java/com/biopatternsg/application/usecase/BuildPubmedPairsUseCase.java`, and `src/main/java/com/biopatternsg/infrastructure/adapters/in/restcontrollers/PubmedController.java`
- [X] T026 [US3] Update `BuildKnowledgeBaseRepoWeb.generateKnowledgeBase(String pipelineId, PubtatorResult pubtatorResult)` and `BuildKnowledgeBaseAdapter` in `src/main/java/com/biopatternsg/domain/ports/out/external_repositories/BuildKnowledgeBaseRepoWeb.java` and `src/main/java/com/biopatternsg/infrastructure/adapters/out/external_repositories/BuildKnowledgeBaseAdapter.java`
- [X] T027 [US3] Update `ConfigAndControlRepository.updateStep(PipelineStepUpdate)`, `ConfigAndControlRepositoryAdapter`, `QueryConfigAndControl`, and `QueryConfigAndControlImpl` in `src/main/java/com/biopatternsg/domain/ports/out/external_repositories/ConfigAndControlRepository.java`, `src/main/java/com/biopatternsg/infrastructure/adapters/out/ConfigAndControlRepositoryAdapter.java`, `src/main/java/com/biopatternsg/infrastructure/internal_services/QueryConfigAndControl.java`, and `src/main/java/com/biopatternsg/infrastructure/internal_services/impl/QueryConfigAndControlImpl.java`
- [X] T028 [US3] Update `KbEventRepository.findByRelation(KbEventKey)` and `upsert(KbEvent)` and `KbEventRepositoryAdapter` in `src/main/java/com/biopatternsg/domain/ports/out/repositories/KbEventRepository.java` and `src/main/java/com/biopatternsg/infrastructure/adapters/out/KbEventRepositoryAdapter.java`
- [X] T029 [US3] Update `KbObjectRepositoryAdapter.upsertKbObject(String pipelineId, KbObjectUpsertCommand command)` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/KbObjectRepositoryAdapter.java`
- [X] T030 [US3] Update all `updateStep`, `generateKnowledgeBase`, and `upsert` callers in `src/main/java/com/biopatternsg/application/usecase/GenerateKbForPipelineUseCase.java`, `src/main/java/com/biopatternsg/application/usecase/BuildPubmedPairsUseCase.java`, `src/main/java/com/biopatternsg/application/usecase/GenerateAlignedObjectsUseCase.java`, `src/main/java/com/biopatternsg/application/usecase/ProcessNcbiSearchRequestUseCase.java`, and `src/main/java/com/biopatternsg/application/usecase/ProcessPubtatorBatchUseCase.java`
- [X] T031 [P] [US3] Update unit tests for encapsulated signatures in `src/test/java/com/biopatternsg/application/usecase/BuildPubmedPairsUseCaseTest.java`, `src/test/java/com/biopatternsg/application/usecase/GenerateKbForPipelineUseCaseTest.java`, `src/test/java/com/biopatternsg/application/usecase/GenerateAlignedObjectsUseCaseTest.java`, `src/test/java/com/biopatternsg/application/usecase/ProcessNcbiSearchRequestUseCaseTest.java`, `src/test/java/com/biopatternsg/application/usecase/ProcessPubtatorBatchUseCaseTest.java`, and `src/test/java/com/biopatternsg/infrastructure/adapters/out/KbEventRepositoryAdapterTest.java`

**Checkpoint**: User Story 3 is complete — zero methods across ports, use cases, services, or adapters exceed 3 parameters.

---

## Phase 5: User Story 4 - Synchronize Use Case Documentation with Updated Contracts (Priority: P3)

**Goal**: Update `.agents/use_cases/*.md` files to reflect updated Inbound Port signatures and new Application Service dependencies.

**Independent Test**: Run `./mvnw test -Dtest=DocumentationGovernanceTest`.

### Implementation for User Story 4

- [X] T032 [P] [US4] Update `.agents/use_cases/BuildPubmedPairsUseCase.md` with `BuildPubmedPairsCommand` contract and `PubmedPairsBuilderService` collaboration
- [X] T033 [P] [US4] Update `.agents/use_cases/GenerateAlignedObjectsUseCase.md` with `ExpertObjectAlignmentService` collaboration and `PipelineStepUpdate`
- [X] T034 [P] [US4] Update `.agents/use_cases/GenerateKbForPipelineUseCase.md` with updated `BuildKnowledgeBaseRepoWeb`, `KbEventRepository`, and `ConfigAndControlRepository` contracts
- [X] T035 [P] [US4] Update `.agents/use_cases/GetKbEventsByRestrictionUseCase.md` to document `RestrictionLevel` filtering in Application and `findByPipelineId` repository usage

**Checkpoint**: User Story 4 is complete — all affected use-case documentation is synchronized.

---

## Phase 6: Polish & Cross-Cutting Verification

**Purpose**: Final compilation, architecture governance, documentation governance, and full unit test validation

- [X] T036 Run full validation gates: `./mvnw clean compile`, `./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest`, and `./mvnw test` in `services/pubmed-integration`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Starts immediately.
- **US1 (Phase 2)** & **US2 (Phase 3)**: Can run sequentially or in parallel (minimal overlap on `BuildPubmedPairsUseCase` / `GenerateAlignedObjectsUseCase` only with US3).
- **US3 (Phase 4)**: Depends on US1 and US2 completion for clean sequential updates to `BuildPubmedPairsUseCase`, `GenerateAlignedObjectsUseCase`, `KbEventRepositoryAdapter`, and `KbObjectRepositoryAdapter`.
- **US4 (Phase 5)**: Depends on US1–US3 contract finalization.
- **Polish (Phase 6)**: Runs after all stories are complete.

### Parallel Opportunities

- **US1**: `T002`, `T003`, `T004` can run in parallel; `T009`, `T010` can run in parallel.
- **US2**: `T011`, `T012` can run in parallel; `T017`, `T018`, `T019`, `T020` can run in parallel.
- **US3**: `T021`, `T022`, `T023`, `T024` can run in parallel.
- **US4**: `T032`, `T033`, `T034`, `T035` can run in parallel.

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 1 (baseline check).
2. Complete Phase 2 (US1: `RestrictionLevel` + `BiologicalTermNormalizer`).
3. Validate US1 unit tests independently.

### Incremental Delivery

1. Complete US1 → clean persistence adapters.
2. Complete US2 → extracted Application Services + thread-safe parallel pair generation.
3. Complete US3 → all >3-param signatures encapsulated into domain records.
4. Complete US4 → use case documentation synchronized.
5. Run Phase 6 final validation gates (`compile`, `ArchitectureLayerTest`, `DocumentationGovernanceTest`, full `test`).

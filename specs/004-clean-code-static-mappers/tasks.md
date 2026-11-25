---

description: "Task list for Clean Code Lambdas, Constructor Injection, Null Safety, Explicit String Representation, and Stateless Mappers"
---

# Tasks: Clean Code Lambdas, Constructor Injection, Null Safety, Explicit String Representation, and Stateless Mappers

**Input**: Design documents from `/specs/004-clean-code-static-mappers/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/mappers-and-dtos.md`, `quickstart.md`

**Tests**: Unit tests are included for static mappers, null-safety (`alignedAs == null`), pagination bounds (`page < 0`, `size <= 0`), and `NcbiESearchResponse`/`NcbiESearchResultDto` `toString()`.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Baseline Verification)

**Purpose**: Confirm clean baseline compilation and test execution

- [X] T001 Verify baseline compilation and tests pass with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw clean test` in `services/pubmed-integration`

---

## Phase 2: User Story 1 - Centralize Data Mapping into Stateless, Null-Safe Pure Mappers (Priority: P1) 🎯 MVP

**Goal**: Convert `PubtatorResultMapper` into a `public final` stateless static mapper, create `AlignedResultMapper`, `KbEventMapper`, `KbObjectMapper`, and `BuildKnowledgeBaseMapper`, and delegate all inline adapter mapping to them.

**Independent Test**: Run unit tests for all static mappers and refactored repository adapters.

### Implementation & Tests for User Story 1

- [X] T002 [P] [US1] Refactor `PubtatorResultMapper` to `public final` with `private` constructor, zero CDI annotations, and `public static` null-safe methods (`pubtatorToModel`, `collectionToModel`, `modelToCollection`, `collectionToModelList`, `modelToCollectionList`, `knowledgeBaseToCollection`) in `src/main/java/com/biopatternsg/infrastructure/mapper/PubtatorResultMapper.java`
- [X] T003 [P] [US1] Create `AlignedResultMapper` (`public final`, `private` constructor, `modelToCollection` guarding `alignedAs == null`, `collectionToSummary`) in `src/main/java/com/biopatternsg/infrastructure/mapper/AlignedResultMapper.java`
- [X] T004 [P] [US1] Create `KbEventMapper` (`public final`, `private` constructor, `collectionToModel`, `collectionToModelList`) in `src/main/java/com/biopatternsg/infrastructure/mapper/KbEventMapper.java`
- [X] T005 [P] [US1] Create `KbObjectMapper` (`public final`, `private` constructor, `collectionToModel`, `collectionToModelList`) in `src/main/java/com/biopatternsg/infrastructure/mapper/KbObjectMapper.java`
- [X] T006 [P] [US1] Create `BuildKnowledgeBaseMapper` (`public final`, `private` constructor, `modelToDocumentRequest`, `buildKbToModel`) in `src/main/java/com/biopatternsg/infrastructure/mapper/BuildKnowledgeBaseMapper.java`
- [X] T007 [US1] Update `PubtatorResponseParser` to call `PubtatorResultMapper.pubtatorToModel(...)` statically without CDI injection in `src/main/java/com/biopatternsg/infrastructure/adapters/in/consumers/PubtatorResponseParser.java`
- [X] T008 [US1] Delegate inline mapping in `PubtatorResultRepositoryAdapter` to `PubtatorResultMapper` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/PubtatorResultRepositoryAdapter.java`
- [X] T009 [P] [US1] Delegate inline mapping in `AlignedResultRepositoryAdapter` to `AlignedResultMapper` and replace `Collectors.toList()` with `.toList()` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/AlignedResultRepositoryAdapter.java`
- [X] T010 [P] [US1] Delegate inline mapping in `KbEventRepositoryAdapter` to `KbEventMapper` and replace `Collectors.toList()` with `.toList()` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/KbEventRepositoryAdapter.java`
- [X] T011 [P] [US1] Delegate 4 duplicated `new KbObject(...)` blocks in `KbObjectRepositoryAdapter` to `KbObjectMapper` and replace `Collectors.toList()` with `.toList()` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/KbObjectRepositoryAdapter.java`
- [X] T012 [P] [US1] Delegate inline mapping in `BuildKnowledgeBaseAdapter` to `BuildKnowledgeBaseMapper` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/external_repositories/BuildKnowledgeBaseAdapter.java`
- [X] T013 [P] [US1] Add unit tests for `AlignedResultMapper`, `KbEventMapper`, `KbObjectMapper`, `BuildKnowledgeBaseMapper`, and `PubtatorResultMapper` in `src/test/java/com/biopatternsg/infrastructure/mapper/StaticMappersTest.java`

**Checkpoint**: User Story 1 is complete — all mappers in `infrastructure.mapper` are stateless static utilities and adapters contain zero inline mapping blocks.

---

## Phase 3: User Story 2 - Simplify Stream Pipelines and Enforce Constructor Dependency Injection (Priority: P2)

**Goal**: Extract multi-line lambdas into private helper methods, convert field injection to explicit constructor injection, and clean up unused annotations and imports.

**Independent Test**: Compile and run full unit test suite confirming constructor injection and clean stream pipelines.

### Implementation for User Story 2

- [X] T014 [P] [US2] Extract multi-line `parallelStream()` lambda into `processPmid(...)` private helper in `src/main/java/com/biopatternsg/application/usecase/GenerateKbForPipelineUseCase.java`
- [X] T015 [P] [US2] Refactor `NcbiQueueSenderRabbitAdapter` to use `private final Emitter<NcbiSearchRequest>` via explicit constructor with `@Channel("ncbi-out")` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/producers/NcbiQueueSenderRabbitAdapter.java`
- [X] T016 [P] [US2] Refactor `PubtatorQueueSenderRabbitAdapter` to use `private final Emitter<PubtatorSearchRequest>` via explicit constructor with `@Channel("pubtator-out")` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/producers/PubtatorQueueSenderRabbitAdapter.java`
- [X] T017 [P] [US2] Refactor `SearchPubtatorByPmidsUseCase` to inject `batchSize` via explicit constructor with `@ConfigProperty(name = "pubtator.batch-size", defaultValue = "50")` in `src/main/java/com/biopatternsg/application/usecase/SearchPubtatorByPmidsUseCase.java`
- [X] T018 [P] [US2] Remove unused `@Slf4j`, `@RequiredArgsConstructor`, and unused `Map` import in `src/main/java/com/biopatternsg/application/services/impl/BiologicalObjectsServiceImpl.java`, `src/main/java/com/biopatternsg/infrastructure/adapters/out/PubmedResultRepositoryAdapter.java`, `src/main/java/com/biopatternsg/infrastructure/adapters/out/PubtatorResultRepositoryAdapter.java`, `src/main/java/com/biopatternsg/infrastructure/adapters/out/PairRepositoryAdapter.java`, and `src/main/java/com/biopatternsg/infrastructure/mongo/KbObjectCollection.java`

**Checkpoint**: User Story 2 is complete — zero field-injected dependencies/properties, zero multi-line lambdas, and zero unused annotations.

---

## Phase 4: User Story 3 - Enforce Null-Safety Guards, Pagination Bounds, and Explicit String Representations (Priority: P2)

**Goal**: Convert `NcbiESearchResponse` and `NcbiESearchResultDto` to Java `record`s for explicit `toString()`, add pagination bounds guards (`page < 0`, `size <= 0`) in `KbObjectRepositoryAdapter.findByPipelineId`, and test edge cases.

**Independent Test**: Run `KbObjectRepositoryAdapterTest` and `NcbiESearchDtoTest` verifying pagination bounds and `toString()` output.

### Implementation & Tests for User Story 3

- [X] T019 [P] [US3] Convert `NcbiESearchResponse` to Java `record` preserving `@JsonIgnoreProperties(ignoreUnknown = true)` and `@JsonProperty` in `src/main/java/com/biopatternsg/infrastructure/clients/dtos/NcbiESearchResponse.java`
- [X] T020 [P] [US3] Convert `NcbiESearchResultDto` to Java `record` preserving `@JsonIgnoreProperties(ignoreUnknown = true)` and `@JsonProperty` in `src/main/java/com/biopatternsg/infrastructure/clients/dtos/NcbiESearchResultDto.java`
- [X] T021 [US3] Update `QueryNcbiESearchImpl` to use record accessors (`eSearchResult()`, `idList()`) in `src/main/java/com/biopatternsg/infrastructure/external_services/impl/QueryNcbiESearchImpl.java`
- [X] T022 [US3] Add `page < 0` and `size <= 0` guards (avoiding division by zero) in `KbObjectRepositoryAdapter.findByPipelineId` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/KbObjectRepositoryAdapter.java`
- [X] T023 [P] [US3] Add unit tests for `KbObjectRepositoryAdapter` pagination bounds (`size <= 0`, `page < 0`) in `src/test/java/com/biopatternsg/infrastructure/adapters/out/KbObjectRepositoryAdapterTest.java`
- [X] T024 [P] [US3] Add unit tests for `NcbiESearchResponse` and `NcbiESearchResultDto` explicit `toString()` in `src/test/java/com/biopatternsg/infrastructure/clients/dtos/NcbiESearchDtoTest.java`

**Checkpoint**: User Story 3 is complete — explicit `toString()` on all client DTOs and full null/pagination safety.

---

## Phase 5: Polish & Cross-Cutting Verification

**Purpose**: Final compilation, architecture governance, documentation governance, and full unit test suite

- [X] T025 Run full validation gates: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw clean compile && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test` in `services/pubmed-integration`

---

## Dependencies & Execution Order

- **Setup (Phase 1)**: Starts immediately.
- **US1 (Phase 2)**, **US2 (Phase 3)**, **US3 (Phase 4)**: Can proceed sequentially P1 → P2 (minimal overlap only on `KbObjectRepositoryAdapter.java` between T011 and T022).
- **Polish (Phase 5)**: Runs after all user stories are complete.

---

description: "Task list for Inbound Consumers, REST Controller and Lifecycle Delivery-Layer Compliance"
---

# Tasks: Inbound Consumers, REST Controller and Lifecycle Delivery-Layer Compliance

**Input**: Design documents from `/specs/002-inbound-delivery-layer-compliance/`
**Prerequisites**: `plan.md` (required), `spec.md` (required for user stories), `research.md`, `data-model.md`, `contracts/inbound-ports-and-endpoints.md`, `quickstart.md`

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., `US1`, `US2`, `US3`)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify baseline compilation and prepare test directories

- [X] T001 Verify baseline build with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw clean compile`
- [X] T002 [P] Create test package directory `src/test/java/com/biopatternsg/infrastructure/session/`

---

## Phase 2: User Story 1 - Delegate Message Consumer Workflows to Dedicated Use Cases (Priority: P1) 🎯 MVP

**Goal**: Extract NCBI and PubTator queue consumer orchestration into dedicated Inbound Ports (`ProcessNcbiSearchRequest`, `ProcessPubtatorBatch`) and Use Cases (`ProcessNcbiSearchRequestUseCase`, `ProcessPubtatorBatchUseCase`), encapsulate PubTator JSON parsing behind `PubtatorSearchRepoWeb` / `PubtatorSearchAdapter`, and add unit tests + Use Case documentation.

**Independent Test**: Verify `NcbiRabbitConsumer` injects only `ProcessNcbiSearchRequest`, `PubtatorRabbitConsumer` injects only `ProcessPubtatorBatch`, `PubtatorSearchRepoWeb.search(List<String>)` returns `List<PubtatorResult>`, and `ArchitectureLayerTest`, `DocumentationGovernanceTest`, `ProcessNcbiSearchRequestUseCaseTest`, and `ProcessPubtatorBatchUseCaseTest` pass.

### Implementation for User Story 1

- [X] T003 [P] [US1] Create Inbound Port `ProcessNcbiSearchRequest` (`void execute(NcbiSearchRequest request)`) in `src/main/java/com/biopatternsg/domain/ports/in/ProcessNcbiSearchRequest.java`
- [X] T004 [P] [US1] Create Inbound Port `ProcessPubtatorBatch` (`void execute(PubtatorSearchRequest request)`) in `src/main/java/com/biopatternsg/domain/ports/in/ProcessPubtatorBatch.java`
- [X] T005 [P] [US1] Update `PubtatorSearchRepoWeb.search(List<String> pmids)` to return `List<PubtatorResult>` in `src/main/java/com/biopatternsg/domain/ports/out/external_repositories/PubtatorSearchRepoWeb.java`
- [X] T006 [US1] Update `PubtatorSearchAdapter` to inject `PubtatorResponseParser` and return `List<PubtatorResult>` from `search(List<String> pmids)` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/external_repositories/PubtatorSearchAdapter.java` (depends on T005)
- [X] T007 [P] [US1] Implement `ProcessNcbiSearchRequestUseCase` with `LogSanitizer.sanitize(...)` logging, PMID persistence, atomic progress completion, pair cleanup, and `SEARCH_PUBMED_IDS` notification in `src/main/java/com/biopatternsg/application/usecase/ProcessNcbiSearchRequestUseCase.java` (depends on T003)
- [X] T008 [P] [US1] Implement `ProcessPubtatorBatchUseCase` with uncached PMID filtering, `PubtatorSearchRepoWeb` call, `hasEvents` filtering, persistence, atomic progress completion, and `SEARCH_PUBTATOR` notification in `src/main/java/com/biopatternsg/application/usecase/ProcessPubtatorBatchUseCase.java` (depends on T004, T005)
- [X] T009 [P] [US1] Refactor `NcbiRabbitConsumer` to inject ONLY `ProcessNcbiSearchRequest`, deserialize `JsonObject`, and delegate to `processNcbiSearchRequest.execute(request)` in `src/main/java/com/biopatternsg/infrastructure/adapters/in/consumers/NcbiRabbitConsumer.java` (depends on T003)
- [X] T010 [P] [US1] Refactor `PubtatorRabbitConsumer` to inject ONLY `ProcessPubtatorBatch`, deserialize `JsonObject`, and delegate to `processPubtatorBatch.execute(request)` in `src/main/java/com/biopatternsg/infrastructure/adapters/in/consumers/PubtatorRabbitConsumer.java` (depends on T004)
- [X] T011 [P] [US1] Create unit tests for `ProcessNcbiSearchRequestUseCase` in `src/test/java/com/biopatternsg/application/usecase/ProcessNcbiSearchRequestUseCaseTest.java` (depends on T007)
- [X] T012 [P] [US1] Create unit tests for `ProcessPubtatorBatchUseCase` in `src/test/java/com/biopatternsg/application/usecase/ProcessPubtatorBatchUseCaseTest.java` (depends on T008)
- [X] T013 [P] [US1] Write English Use Case documentation with Mermaid `sequenceDiagram` for `ProcessNcbiSearchRequestUseCase` in `.agents/use_cases/ProcessNcbiSearchRequestUseCase.md`
- [X] T014 [P] [US1] Write English Use Case documentation with Mermaid `sequenceDiagram` for `ProcessPubtatorBatchUseCase` in `.agents/use_cases/ProcessPubtatorBatchUseCase.md`
- [X] T015 [US1] Index `ProcessNcbiSearchRequestUseCase.md` and `ProcessPubtatorBatchUseCase.md` in `.agents/use_cases/README.md` and Section 5 of `.agents/architecture.md` (depends on T013, T014)

**Checkpoint**: User Story 1 complete — both consumers delegate strictly to Inbound Ports, both new Use Cases are tested and documented, and `DocumentationGovernanceTest` passes.

---

## Phase 3: User Story 2 - Clean Up REST Controller Naming, Async Dispatch, and Request Validation (Priority: P1)

**Goal**: Rename `buildTreeMesh` to `buildPubmedPairs`, extract `runAsyncTask(String taskName, Runnable action)` in `PubmedController`, and apply declarative Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Valid`) while preserving HTTP status codes.

**Independent Test**: Verify `buildTreeMesh` no longer exists, all 5 async endpoints use `runAsyncTask`, declarative validation annotations are present, and `./mvnw test` passes.

### Implementation for User Story 2

- [X] T016 [US2] Rename `buildTreeMesh` to `buildPubmedPairs` in `src/main/java/com/biopatternsg/infrastructure/adapters/in/restcontrollers/PubmedController.java`
- [X] T017 [US2] Extract private helper `runAsyncTask(String taskName, Runnable action)` and replace all 5 duplicated `CompletableFuture.runAsync` blocks in `src/main/java/com/biopatternsg/infrastructure/adapters/in/restcontrollers/PubmedController.java` (depends on T016)
- [X] T018 [US2] Apply declarative Jakarta Bean Validation (`@NotBlank` on path params `name`/`term`/`pipelineId`, `@NotNull` on `rolesMap`, `@Valid` on `KbEventsByRestrictionRequest`) and remove redundant manual blank checks in `src/main/java/com/biopatternsg/infrastructure/adapters/in/restcontrollers/PubmedController.java` (depends on T017)

**Checkpoint**: User Story 2 complete — `PubmedController` is clean, concise, and declaratively validated.

---

## Phase 4: User Story 3 - Harden Session Context Extraction and Startup Lifecycle Resumption (Priority: P2)

**Goal**: Make `SessionUtils.getUserId()` null-safe and update `AppLifecycleBean` to use `@Slf4j`, log `DEFAULT_DELAY_SECONDS` dynamically, and close JAX-RS `Response` via `try-with-resources`.

**Independent Test**: Verify `SessionUtilsTest` covers null context, missing `x-user-id`, empty `x-user-id`, and valid `x-user-id`, and `AppLifecycleBeanTest` verifies `Response.close()` is invoked.

### Implementation for User Story 3

- [X] T019 [P] [US3] Harden `SessionUtils.getUserId()` against null `context` and missing/empty `x-user-id` header in `src/main/java/com/biopatternsg/infrastructure/session/SessionUtils.java`
- [X] T020 [P] [US3] Refactor `AppLifecycleBean` to use `@Slf4j`, log `DEFAULT_DELAY_SECONDS` dynamically, and wrap `resumePipelines(...)` in `try (Response response = ...)` in `src/main/java/com/biopatternsg/infrastructure/boot_functions/AppLifecycleBean.java`
- [X] T021 [P] [US3] Create `SessionUtilsTest` covering null context, missing header, empty header, and valid header in `src/test/java/com/biopatternsg/infrastructure/session/SessionUtilsTest.java` (depends on T019)
- [X] T022 [P] [US3] Update `AppLifecycleBeanTest` to verify `Response.close()` is called on startup resumption in `src/test/java/com/biopatternsg/infrastructure/boot_functions/AppLifecycleBeanTest.java` (depends on T020)

**Checkpoint**: User Story 3 complete — session extraction and startup resumption are null-safe and leak-free.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Run full verification suite from `quickstart.md`

- [X] T023 Run full validation suite: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw clean test` (including `ArchitectureLayerTest` and `DocumentationGovernanceTest`) and verify `buildTreeMesh` is absent from `src/main/java/`

---

## Dependencies & Execution Order

- **Setup (Phase 1)**: Start immediately (`T001`–`T002`).
- **User Story 1 (Phase 2)**: Depends on Setup. `T003`–`T005`, `T013`–`T014` can start in parallel.
- **User Story 2 (Phase 3)**: Depends on Setup; independent of US1 and US3. `T016` → `T017` → `T018` sequentially on `PubmedController.java`.
- **User Story 3 (Phase 4)**: Depends on Setup; independent of US1 and US2. `T019` and `T020` run in parallel, followed by `T021` and `T022`.
- **Polish (Phase 5)**: Depends on US1, US2, and US3.

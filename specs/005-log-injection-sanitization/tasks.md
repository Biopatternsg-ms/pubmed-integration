---

description: "Task list for Log Injection Prevention and Logging Sanitization"
---

# Tasks: Log Injection Prevention and Logging Sanitization

**Input**: Design documents from `/specs/005-log-injection-sanitization/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/logging-sanitization.md`, `quickstart.md`

**Tests**: Unit tests in `LogSanitizerTest` are included for scalar and collection sanitization.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Baseline Verification)

**Purpose**: Confirm clean baseline compilation and test execution

- [X] T001 Verify baseline compilation and tests pass with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw clean test` in `services/pubmed-integration`

---

## Phase 2: User Story 1 - Sanitize All Untrusted and External Inputs in Application Logs (Priority: P1) 🎯 MVP

**Goal**: Add `sanitizeCollection` to `LogSanitizer` with unit tests, and wrap every untrusted/external log argument and formatted `e.getMessage()` across all use cases, services, RabbitMQ producers, repository adapters, and external service wrappers in `LogSanitizer.sanitize(...)` / `LogSanitizer.sanitizeCollection(...)`.

**Independent Test**: Run `LogSanitizerTest` and verify zero unsanitized external log arguments remain via `grep` and unit tests.

### Implementation & Tests for User Story 1

- [X] T002 [P] [US1] Add `sanitizeCollection(Collection<?>)` to `src/main/java/com/biopatternsg/domain/util/LogSanitizer.java`
- [X] T003 [P] [US1] Add unit tests for `sanitizeCollection` (including `null`, empty, and CRLF elements) in `src/test/java/com/biopatternsg/domain/util/LogSanitizerTest.java`
- [X] T004 [P] [US1] Sanitize `pipelineId`, `term`, and `e.getMessage()` in `src/main/java/com/biopatternsg/application/usecase/SearchPubmedByPairsUseCase.java`
- [X] T005 [P] [US1] Sanitize `pipelineId` and `e.getMessage()` in `src/main/java/com/biopatternsg/application/usecase/SearchPubtatorByPmidsUseCase.java`
- [X] T006 [P] [US1] Sanitize `pmid`, `kbEvent` fields, and `e.getMessage()` in `src/main/java/com/biopatternsg/application/usecase/GenerateKbForPipelineUseCase.java`
- [X] T007 [P] [US1] Sanitize `request`, `result.term()`, `result.ids()`, and `e.getMessage()` in `src/main/java/com/biopatternsg/application/usecase/ProcessNcbiSearchRequestUseCase.java`
- [X] T008 [P] [US1] Sanitize `request`, `pipelineId`, and `e.getMessage()` in `src/main/java/com/biopatternsg/application/usecase/ProcessPubtatorBatchUseCase.java`
- [X] T009 [P] [US1] Sanitize `currentBiologicalObject.id()` in `src/main/java/com/biopatternsg/application/services/impl/PubmedPairsBuilderServiceImpl.java`
- [X] T010 [P] [US1] Sanitize `request.term()` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/producers/NcbiQueueSenderRabbitAdapter.java`
- [X] T011 [P] [US1] Sanitize `request.pipelineId()` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/producers/PubtatorQueueSenderRabbitAdapter.java`
- [X] T012 [P] [US1] Sanitize `pmidsParam` in `src/main/java/com/biopatternsg/infrastructure/external_services/impl/QueryPubtatorImpl.java`
- [X] T013 [P] [US1] Sanitize `pubmedIds` and `e.getMessage()` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/KbEventRepositoryAdapter.java`
- [X] T014 [P] [US1] Sanitize `processedSynonyms`, `uniqueBiotypes`, and `e.getMessage()` in `src/main/java/com/biopatternsg/infrastructure/adapters/out/KbObjectRepositoryAdapter.java`

**Checkpoint**: User Story 1 is complete — all untrusted/external log arguments are sanitized.

---

## Phase 3: User Story 2 - Fix Unconsumed Stream Logging in Object Alignment Diagnostics (Priority: P2)

**Goal**: Replace the unconsumed `Stream` log in `GenerateAlignedObjectsUseCase` with a collected, sanitized list of symbol strings.

**Independent Test**: Run `GenerateAlignedObjectsUseCaseTest` and verify `LogSanitizer.sanitizeCollection(...)` is used for expert object symbols.

### Implementation for User Story 2

- [X] T015 [US2] Replace `safeExpertObjects.stream().map(ExpertObjectConfig::symbol)` with `LogSanitizer.sanitizeCollection(safeExpertObjects.stream().map(ExpertObjectConfig::symbol).toList())` in `src/main/java/com/biopatternsg/application/usecase/GenerateAlignedObjectsUseCase.java`

**Checkpoint**: User Story 2 is complete — alignment diagnostics log a readable, sanitized list of symbols.

---

## Phase 4: Polish & Cross-Cutting Verification

**Purpose**: Final compilation, architecture governance, documentation governance, and full unit test suite

- [X] T016 Run full validation gates: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw clean compile && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test` in `services/pubmed-integration`

---

## Dependencies & Execution Order

- **Setup (Phase 1)**: Starts immediately.
- **US1 (Phase 2)** & **US2 (Phase 3)**: `T002` (`LogSanitizer.sanitizeCollection`) should complete before `T013`, `T014`, `T015`; `T004`–`T012` can run in parallel immediately.
- **Polish (Phase 4)**: Runs after all user stories are complete.

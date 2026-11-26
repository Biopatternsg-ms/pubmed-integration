---

description: "Task list for Architecture Governance Hardening"
---

# Tasks: Architecture Governance Hardening

**Input**: Design documents from `/specs/006-archunit-governance-hardening/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/archunit-rules.md`, `quickstart.md`

**Tests**: All tasks in this feature implement automated ArchUnit governance tests in `ArchitectureLayerTest.java`.

**Organization**: Tasks are grouped by user story to enable independent verification of each governance category.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Baseline Verification)

**Purpose**: Confirm clean baseline compilation and existing governance tests

- [X] T001 Verify baseline compilation and governance tests pass with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest` in `services/pubmed-integration`

---

## Phase 2: User Story 1 - Enforce Hexagonal Layer and Framework Isolation Automatically (Priority: P1) 🎯 MVP

**Goal**: Add ArchUnit rules enforcing framework isolation in Domain and Application and prohibiting Use Cases from depending on `application.services.impl..`.

**Independent Test**: Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test -Dtest=ArchitectureLayerTest`.

### Implementation for User Story 1

- [X] T002 [US1] Add `domainShouldNotDependOnFrameworksTest` rule to `src/test/java/archtest/ArchitectureLayerTest.java`
- [X] T003 [US1] Add `applicationShouldNotDependOnDeliveryOrPersistenceFrameworksTest` rule to `src/test/java/archtest/ArchitectureLayerTest.java`
- [X] T004 [US1] Add `useCasesShouldNotDependOnServiceImplementationsTest` rule to `src/test/java/archtest/ArchitectureLayerTest.java`

**Checkpoint**: User Story 1 is complete — Domain/Application framework isolation and Use Case abstraction boundaries are enforced.

---

## Phase 3: User Story 2 - Enforce Stateless Mappers, Constructor Injection, and Inbound Adapter Boundaries (Priority: P2)

**Goal**: Add ArchUnit rules enforcing stateless `final` mappers with `private` constructors, zero field injection in `application..` and `infrastructure.adapters..`, and zero direct dependencies from inbound adapters on `domain.ports.out..`.

**Independent Test**: Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest`.

### Implementation for User Story 2

- [X] T005 [US2] Add `staticMappersShouldBeStatelessFinalUtilityClassesTest` rule to `src/test/java/archtest/ArchitectureLayerTest.java`
- [X] T006 [US2] Add `noFieldInjectionInApplicationAndAdaptersTest` rule to `src/test/java/archtest/ArchitectureLayerTest.java`
- [X] T007 [US2] Add `inboundAdaptersShouldNotDependOnOutboundPortsTest` rule to `src/test/java/archtest/ArchitectureLayerTest.java`

**Checkpoint**: User Story 2 is complete — all 13 ArchUnit governance rules are active.

---

## Phase 4: Polish & Cross-Cutting Verification

**Purpose**: Final compilation, all 13 ArchUnit + DocumentationGovernance tests, and full unit test suite

- [X] T008 Run full validation gates: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw clean compile && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test` in `services/pubmed-integration`

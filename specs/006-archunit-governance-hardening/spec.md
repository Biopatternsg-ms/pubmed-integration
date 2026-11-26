# Feature Specification: Architecture Governance Hardening

**Feature Branch**: `feature/06-architecture-governance-archunit-hardening`

**Created**: 2026-11-25

**Status**: Draft

**Input**: User description: "/speckit-specify basado en /home/yacson/Proyectos/biopatternsg-ms/SDD/pubmed-integrations/06-architecture-governance-archunit-hardening"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Enforce Hexagonal Layer and Framework Isolation Automatically (Priority: P1)

As an architect and team lead, I want automated architecture governance checks to verify that the core domain layer has zero dependencies on external delivery, serialization, persistence, messaging, or configuration frameworks, and that the application layer remains free of delivery and persistence framework dependencies and concrete service implementations, so that architectural regressions are caught immediately during builds.

**Why this priority**: Without automated build-time enforcement, framework imports or concrete implementation couplings can silently re-enter the domain or application layers over time.

**Independent Test**: Can be fully tested by running the architecture governance test suite and verifying that all layer-isolation and framework-purity rules pass on the remediated codebase.

**Acceptance Scenarios**:

1. **Given** the core domain layer, **When** automated architecture governance checks run, **Then** zero dependencies on web, dependency-injection, serialization, persistence, messaging, XML/DOM, or exception-handler frameworks are permitted.
2. **Given** the application layer and its use cases, **When** automated architecture governance checks run, **Then** zero dependencies on web, messaging, or persistence frameworks and zero dependencies on concrete application-service implementations are permitted.

---

### User Story 2 - Enforce Stateless Mappers, Constructor Injection, and Inbound Adapter Boundaries (Priority: P2)

As a maintainer, I want automated governance checks to verify that all mappers are stateless non-instantiable utilities, that application and adapter components do not use field injection, and that inbound delivery adapters never bypass use cases to call outbound ports directly, so that clean-code and hexagonal boundaries remain permanently enforced.

**Why this priority**: Enforcing mapper statelessness, constructor injection, and inbound-adapter boundaries automatically prevents future contributors from reintroducing field injection, stateful mappers, or controller-to-repository shortcuts.

**Independent Test**: Can be fully tested by executing the architecture and documentation governance test suites and confirming all new and existing rules pass.

**Acceptance Scenarios**:

1. **Given** all mapper classes in the mapping layer, **When** governance checks run, **Then** every top-level mapper is final, has no container-managed scope annotations, and exposes only private constructors.
2. **Given** all application and adapter classes, **When** governance checks run, **Then** zero fields use dependency or configuration field injection.
3. **Given** all inbound delivery adapters (controllers and message consumers), **When** governance checks run, **Then** zero direct dependencies on outbound ports exist.

---

### Edge Cases

- What happens if a use case or adapter introduces a new helper class or synthetic inner class in a governed package? Naming and constructor/modifier rules should handle top-level classes vs. compiler-generated/nested types cleanly without false positives.
- What happens if a new use case is added without matching documentation? The documentation governance check continues to fail the build until documentation is synchronized.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST automatically verify that the core domain layer has zero dependencies on web, dependency-injection, serialization, persistence, messaging, configuration, XML/DOM, or exception-handler frameworks.
- **FR-002**: System MUST automatically verify that the application layer has zero dependencies on delivery, messaging, persistence, or HTTP exception-handler frameworks.
- **FR-003**: System MUST automatically verify that use cases depend only on application-service abstractions and never on concrete service implementation classes.
- **FR-004**: System MUST automatically verify that all top-level mapper classes are final, carry no container lifecycle scope annotations, and declare only private constructors.
- **FR-005**: System MUST automatically verify that application and adapter components contain no field-injected dependencies or configuration properties.
- **FR-006**: System MUST automatically verify that inbound delivery adapters (REST controllers and message consumers) do not depend directly on outbound ports.
- **FR-007**: System MUST keep existing use-case documentation governance checks active and passing alongside all architecture rules.
- **FR-008**: System MUST pass full compilation, architecture/documentation governance checks, and the complete unit test suite with zero failures.

### Key Entities

- **Architecture Governance Suite**: Automated build-time rule set verifying layer boundaries, framework purity, naming conventions, constructor injection, and mapper statelessness.
- **Documentation Governance Suite**: Automated build-time rule set verifying 100% alignment between implemented use cases and their documentation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 13 automated architecture governance rules and 1 documentation governance check execute and pass with 0 failures and 0 skipped checks.
- **SC-002**: 100% of full compilation and unit test suites pass with zero regressions after hardening the governance suite.

## Assumptions

- Remediations from Requirements 01–05 are already in place on the target codebase so all 6 new rules pass immediately.
- Modifying CI/pipeline definitions or adding live container integration tests remains out of scope.

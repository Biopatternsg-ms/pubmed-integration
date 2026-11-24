# Feature Specification: Domain and Application Layer Purity and MongoDB Decoupling

**Feature Branch**: `001-domain-purity-mongo-decoupling`

**Created**: 2026-11-24

**Status**: Draft

**Input**: User description: "/speckit-specify basado en /home/yacson/Proyectos/biopatternsg-ms/SDD/pubmed-integrations/01-domain-and-application-layer-purity-and-mongo-decoupling"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Isolate Persistence Entities Within Infrastructure (Priority: P1)

As a service architect and maintainer, I need all database persistence entities to live exclusively within the infrastructure boundary so that architectural governance checks can reliably enforce separation between business logic and storage concerns.

**Why this priority**: As long as persistence entities reside in a top-level package outside infrastructure, automated boundary checks cannot guarantee that the domain and application cores remain independent of database technology.

**Independent Test**: Can be tested independently by verifying that zero persistence entity definitions remain outside the infrastructure boundary, all existing storage collection names and document field mappings are unchanged, and the service compiles and passes its test suite.

**Acceptance Scenarios**:

1. **Given** the service codebase with persistence entities located in a top-level persistence package, **When** all persistence entities are relocated under the infrastructure boundary, **Then** zero persistence entity classes remain outside infrastructure and all existing database collection names and document schemas remain identical.
2. **Given** repository adapters and tests that reference the persistence entities, **When** their references are updated to the infrastructure location, **Then** the full build and existing test suite pass without regressions.

---

### User Story 2 - Decouple Domain Ports and Use Cases from Persistence Entities (Priority: P1)

As a service developer, I need the pair-building and search-progress workflows and their repository contracts to exchange pure domain concepts instead of database entity types so that business rules can evolve and be tested without database coupling.

**Why this priority**: Domain outbound ports and core use cases currently construct, inspect, and return database entity objects directly, which is the primary violation of hexagonal layer isolation.

**Independent Test**: Can be tested independently by verifying that the pair and search-progress repository contracts, pair-building use case, and pair-search use case reference only pure domain models, while persistence adapters translate between domain models and database entities.

**Acceptance Scenarios**:

1. **Given** the pair-building and pair-search workflows, **When** term pairs are created, persisted, and retrieved by pipeline identifier, **Then** the use cases and repository contracts operate exclusively on pure domain models while storing and retrieving the exact same underlying database records.
2. **Given** the search-progress and PubTator search-progress tracking contracts, **When** progress counters are incremented and retrieved for a pipeline, **Then** the contracts return a pure domain search-progress representation (including completion status) mapped by the persistence adapters.

---

### User Story 3 - Decouple Expert Configuration Domain Model from Serialization Annotations (Priority: P2)

As an API consumer and service maintainer, I need the expert object configuration business model to be free of JSON serialization directives while keeping the aligned-objects endpoint tolerant of unknown request fields.

**Why this priority**: Embedding serialization directives in a domain model leaks transport concerns into the core, but removing them without a transport-layer boundary model would break backward compatibility for clients sending extra JSON properties.

**Independent Test**: Can be tested independently by verifying that the expert configuration domain model has no serialization library dependencies and that submitting a request with unknown JSON properties to the aligned-objects endpoint still succeeds with identical behavior.

**Acceptance Scenarios**:

1. **Given** an incoming request to generate aligned objects containing known and unknown JSON properties in expert object configurations, **When** the delivery boundary receives the request, **Then** unknown properties are ignored without error and the mapped pure domain configuration is passed to the application layer.
2. **Given** the domain layer source files, **When** inspected for serialization framework dependencies, **Then** zero domain files depend on JSON serialization annotations or libraries.

---

### User Story 4 - Decouple Alignment Workflow from HTTP Exception Framework (Priority: P2)

As a service maintainer, I need the aligned-objects workflow to signal failures using a pure domain exception rather than an HTTP status exception so that application logic remains transport-agnostic.

**Why this priority**: Throwing HTTP-specific exceptions inside an application use case couples business orchestration to the web delivery framework and prevents reuse across non-HTTP triggers.

**Independent Test**: Can be tested independently by triggering an alignment failure in the use case and verifying that a domain exception is raised by the application layer and translated into the expected HTTP error response at the delivery boundary.

**Acceptance Scenarios**:

1. **Given** a failure during aligned-object generation in the application layer, **When** the use case aborts, **Then** it throws a pure domain exception with no dependency on the HTTP exception handler framework.
2. **Given** the domain exception raised by the application layer during an HTTP request, **When** it reaches the delivery boundary, **Then** the service maps it consistently to an internal server error HTTP response.

---

### Edge Cases

- What happens when a pipeline has zero term pairs or empty search progress? The domain models and mappers must preserve empty collections and zero counts without returning null references.
- What happens when an incoming aligned-objects request includes unrecognized fields inside nested expert configuration objects? The delivery boundary must ignore unknown fields just as before, without propagating transport annotations into the domain.
- What happens when persistence adapters map null or missing database records? Adapters and mappers must handle null inputs safely without throwing unexpected null-pointer errors.
- What happens when existing database documents created prior to this change are read after the refactoring? All collection names and field mappings must remain 100% compatible with existing stored data.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST relocate all 9 persistence collection entities from the top-level persistence package into the infrastructure persistence package while preserving every database collection name and document field schema unchanged.
- **FR-002**: System MUST update all repository adapters, unit tests, and architecture documentation references to use the relocated infrastructure persistence package.
- **FR-003**: System MUST define pure, framework-free domain models for term pairs (or pipeline pair batches) and search progress (including pipeline identifier, completed count, total count, user identifier, and completion check).
- **FR-004**: System MUST refactor the pair repository, search-progress repository, and PubTator search-progress repository contracts to accept and return only pure domain models.
- **FR-005**: System MUST update the pair, search-progress, and PubTator search-progress repository adapters to translate between infrastructure persistence entities and pure domain models using stateless mappers.
- **FR-006**: System MUST refactor the pair-building and pair-search use cases to operate exclusively on pure domain models without importing or instantiating persistence entities.
- **FR-007**: System MUST remove all JSON serialization annotations and imports from the expert object configuration domain model.
- **FR-008**: System MUST introduce a delivery-layer request representation for expert object configurations that ignores unknown JSON properties and maps cleanly to the pure domain model at the inbound boundary.
- **FR-009**: System MUST remove HTTP exception handler imports and exception instantiation from the aligned-objects use case and replace them with a pure domain exception.
- **FR-010**: System MUST ensure the delivery boundary translates the alignment domain exception consistently into the appropriate HTTP error response so external error behavior remains unchanged.

### Key Entities

- **Term Pair / Pipeline Pairs Batch**: Represents an association between two biological/search terms generated for a given pipeline execution, independent of how pairs are stored.
- **Search Progress**: Represents the execution progress of a search pipeline, capturing the pipeline identifier, completed count, total count, initiating user identifier, and whether all expected items have completed.
- **Expert Object Configuration**: Represents user-supplied configuration parameters for generating aligned biological objects, pure of transport serialization metadata.
- **Alignment Failure (Domain Exception)**: Represents an unrecoverable domain/application error occurring during aligned-object generation, independent of HTTP or transport protocols.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of persistence collection entities (9 of 9) reside exclusively inside the infrastructure layer, with 0 persistence entities remaining in top-level packages outside infrastructure.
- **SC-002**: 0 files in the domain layer contain imports from persistence entity packages, serialization libraries, web frameworks, reactive/runtime frameworks, database driver types, or HTTP exception frameworks.
- **SC-003**: 0 files in the application layer contain imports from persistence entity packages, infrastructure packages, or HTTP exception frameworks.
- **SC-004**: 100% of existing external HTTP endpoint paths, JSON request/response contracts (including tolerance of unknown JSON properties), and database collection/field schemas remain backward-compatible.
- **SC-005**: 100% of automated compilation, architectural boundary checks, documentation governance checks, and unit tests pass after all changes are applied.

## Assumptions

- Existing database collections (`pairs`, `pubmed_results`, `search_progress`, `pubtator_results`, `pubtator_search_progress`, `kb_events`, `kb_objects`, `alignedResults`) and their stored document schemas must not be migrated or altered.
- External REST endpoint paths and JSON wire formats consumed by upstream clients must remain unchanged.
- Existing automated architectural boundary and documentation governance tests in the repository are the authoritative verification gates for layer isolation and documentation currency.

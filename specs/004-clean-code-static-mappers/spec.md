# Feature Specification: Clean Code Lambdas, Constructor Injection, Null Safety, Explicit String Representation, and Stateless Mappers

**Feature Branch**: `feature/04-clean-code-lambdas-di-null-safety-tostring-and-static-mappers`

**Created**: 2026-11-25

**Status**: Draft

**Input**: User description: "/speckit-specify basado en /home/yacson/Proyectos/biopatternsg-ms/SDD/pubmed-integrations/04-clean-code-lambdas-di-null-safety-tostring-and-static-mappers"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Centralize Data Mapping into Stateless, Null-Safe Pure Mappers (Priority: P1)

As a maintainer, I want all conversions between external/storage representations and core domain models to live in dedicated, stateless, null-safe mapper utilities rather than being duplicated inline inside adapters or managed as stateful service beans, so that mapping logic is deterministic, reusable, and testable in isolation.

**Why this priority**: Currently, ~150 lines of mapping logic are embedded inside storage adapters, object mapping blocks are duplicated across four places, and one mapper is managed as an injectable service instance. Centralizing mapping into pure, stateless functions eliminates duplication and prevents accidental state bugs.

**Independent Test**: Can be fully tested by invoking each mapper's conversion functions directly with valid, partial, empty, and null inputs without starting any container or database.

**Acceptance Scenarios**:

1. **Given** any persistence or external-service adapter converting between domain models and storage/client representations, **When** mapping is refactored into dedicated stateless mappers, **Then** adapters delegate all conversions to pure, null-safe static mapping functions with zero duplicated mapping blocks.
2. **Given** a mapper receives `null` inputs or `null` nested collections, **When** a conversion function is invoked, **Then** it handles `null` gracefully (returning `null` or empty collections as appropriate) without throwing null-pointer exceptions.

---

### User Story 2 - Simplify Stream Pipelines and Enforce Constructor Dependency Injection (Priority: P2)

As a developer, I want complex multi-line stream/lambda expressions extracted into small single-purpose helper methods, legacy mutable list collectors replaced with immutable equivalents, and all component dependencies provided exclusively through explicit constructors, so that business flows are concise, predictable, and easy to unit-test without reflection tricks.

**Why this priority**: Multi-line inline lambdas obscure control flow and error handling, while field-injected dependencies and configuration values hide a component's true collaborators and complicate isolated unit testing.

**Independent Test**: Can be verified by inspecting all use cases, services, and adapters for single-expression lambdas, immutable list collection, explicit constructor-only dependency/configuration injection, and running the unit test suite.

**Acceptance Scenarios**:

1. **Given** use cases or adapters containing multi-line lambdas or legacy mutable list collectors, **When** refactored, **Then** multi-line blocks are extracted into named single-responsibility helper methods and list transformations produce unmodifiable lists cleanly.
2. **Given** messaging producers, use cases, and internal service clients using field-level dependency or configuration injection or redundant annotations, **When** refactored, **Then** all dependencies and configuration values are supplied through explicit constructors and unused annotations or imports are removed.

---

### User Story 3 - Enforce Null-Safety Guards, Pagination Bounds, and Explicit String Representations (Priority: P2)

As an operator and developer, I want external client data-transfer models to provide clear, explicit string representations for diagnostics and all collection/pagination operations to guard against null lists and invalid page sizes, so that logs remain informative and edge-case inputs never cause runtime arithmetic or null-pointer failures.

**Why this priority**: Missing string representations produce opaque object memory hashes in logs, while unguarded null lists or non-positive page sizes can crash alignment persistence or paginated queries in production.

**Independent Test**: Can be verified via unit tests covering null nested lists during alignment persistence, zero/negative page sizes and negative page indices during paginated object retrieval, and non-default `toString()` output on external search response models.

**Acceptance Scenarios**:

1. **Given** an external search response model instance, **When** its string representation is requested, **Then** it outputs a structured representation of its field values rather than a default object identity hash.
2. **Given** an alignment result with a `null` alternative-alignments list, **When** persisted, **Then** the system treats it as an empty list without throwing a null-pointer error.
3. **Given** a paginated knowledge-base object query with a negative page index or a zero/negative page size, **When** executed, **Then** the query guards against division by zero and invalid offsets safely.

---

### Edge Cases

- What happens when a mapper receives a `null` domain object, `null` storage document, or `null` list of items? Null single objects return `null` (or safe empty summaries); null lists return empty unmodifiable lists.
- What happens when paginated object retrieval receives `page < 0` or `size <= 0`? Negative page indices clamp safely (e.g., to `0`), and non-positive page sizes avoid division by zero and return safe empty or bounded pagination metadata.
- What happens when external search response models have `null` or missing optional fields in JSON payloads? Deserialization ignores unknown fields and preserves existing field-name mappings while still rendering a readable string representation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST implement every mapper in the mapping layer as a stateless, non-instantiable utility exposing only pure, null-safe conversion functions with zero container lifecycle annotations.
- **FR-002**: System MUST extract all inline mapping logic from persistence and external-service adapters into dedicated stateless mappers (covering publication annotations, alignment results, knowledge-base events, knowledge-base objects, and knowledge-base generation payloads).
- **FR-003**: System MUST replace multi-line lambda expressions in use cases and adapters with single-responsibility private helper methods.
- **FR-004**: System MUST replace legacy mutable list collection calls with standard unmodifiable list collection where immutability is intended.
- **FR-005**: System MUST supply all component dependencies and configuration properties exclusively via explicit constructors, eliminating field injection and redundant injection annotations.
- **FR-006**: System MUST remove unused logger, constructor, and import declarations across refactored classes.
- **FR-007**: System MUST ensure all external search client data-transfer models define an explicit, readable string representation while preserving existing serialization/deserialization field mappings.
- **FR-008**: System MUST guard against `null` alternative-alignment lists when persisting alignment results.
- **FR-009**: System MUST guard against negative page numbers and zero or negative page sizes in paginated knowledge-base object retrieval to prevent division-by-zero errors.
- **FR-010**: System MUST pass full compilation, architecture-governance, documentation-governance, and unit test suites after all refactorings.

### Key Entities

- **Stateless Mapper Utilities**: Pure conversion utilities translating between storage/external representations and core domain models without holding mutable state or container-managed lifecycle.
- **External Search Response Models**: Immutable or structured data-transfer models representing search responses from external literature services with explicit string formatting.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of mapper classes are stateless, non-instantiable utilities with zero container-managed instance methods.
- **SC-002**: Zero multi-line lambda blocks and zero legacy mutable list collectors remain in refactored use cases and adapters.
- **SC-003**: Zero field-injected dependencies or configuration properties remain in application and adapter components.
- **SC-004**: 100% of external search response data-transfer models produce explicit field-value string representations.
- **SC-005**: 100% of null-safety and pagination boundary edge cases pass automated unit tests, and 100% of compilation, architecture-governance, documentation-governance, and unit test suites pass with zero regressions.

## Assumptions

- Third-party object-mapping frameworks and external resilience libraries remain out of scope.
- Existing external REST endpoints, RabbitMQ message formats, and MongoDB document schemas remain backward-compatible.

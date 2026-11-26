# Feature Specification: Outbound Adapters Business-Logic Extraction and Parameter Encapsulation

**Feature Branch**: `feature/03-outbound-adapters-business-logic-extraction-and-parameter-encapsulation`

**Created**: 2026-11-25

**Status**: Draft

**Input**: User description: "/speckit-specify basado en /home/yacson/Proyectos/biopatternsg-ms/SDD/pubmed-integrations/03-outbound-adapters-business-logic-extraction-and-parameter-encapsulation"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Extract Domain Filtering and Term Normalization from Persistence Adapters (Priority: P1)

As a system maintainer and researcher, I want knowledge-base event restriction filtering and biological identifier normalization to live in the core business and application layers rather than inside data-storage adapters, so that business rules are consistent, single-sourced, and independently verifiable without requiring a live database.

**Why this priority**: Mixing business rules into persistence adapters duplicates normalization logic across multiple storage components and prevents testing restriction rules in isolation. Centralizing these rules eliminates drift and protects architectural boundaries.

**Independent Test**: Can be fully tested by verifying that restriction filtering (unrestricted, restricted, very restricted, and default/blank inputs) and biological term normalization (prefixed biological identifiers, plain terms, and blank/empty inputs) produce identical, deterministic results without invoking any persistence mechanism.

**Acceptance Scenarios**:

1. **Given** a pipeline has stored knowledge-base events with varying restriction attributes, **When** events are requested for a specific restriction level (`UNRESTRICTED`, `RESTRICTED`, `VERY_RESTRICTED`, `NO_RESTRICTED`, or blank/unspecified), **Then** the application layer applies the restriction rule and returns only the matching events while the storage adapter performs only data retrieval.
2. **Given** a biological term or synonym contains a biotype prefix and suffix (e.g., `@BIOTYPE_TermName`), **When** the term is normalized by the shared domain utility, **Then** both the optional biotype prefix and the clean term suffix are extracted consistently across event and object persistence flows without duplicated parsing rules.
3. **Given** a biological term is plain text without a prefix, or is blank or missing, **When** normalized, **Then** the utility handles it safely without errors and returns a predictable normalized representation.

---

### User Story 2 - Encapsulate Complex Pair Generation and Alignment Algorithms with Thread-Safe Concurrency (Priority: P1)

As a pipeline operator, I want publication pair generation and expert-object alignment algorithms extracted from orchestration use cases into dedicated application services with thread-safe concurrent processing, so that concurrent pair generation never loses data or fails with concurrency errors and orchestration flows remain easy to read and test.

**Why this priority**: The current pair-generation flow mutates a non-thread-safe collection inside a parallel processing stream, creating a live data-loss and runtime-failure risk during concurrent execution. Extracting both algorithms into dedicated services resolves the concurrency defect and separates orchestration from algorithmic computation.

**Independent Test**: Can be fully tested by running pair generation concurrently across multi-member biological object families to verify zero lost pairs or concurrency exceptions, and by running expert-object alignment on sample synonym datasets to verify exact alignment scores and mappings.

**Acceptance Scenarios**:

1. **Given** a biological object family with multiple levels and synonyms, **When** publication pairs are generated using parallel processing, **Then** all unique ordered pairs are collected deterministically in a thread-safe manner with zero lost entries or concurrency failures.
2. **Given** a set of knowledge-base objects and expert-defined biological objects, **When** alignment is requested, **Then** a dedicated alignment service normalizes symbols, builds an inverted synonym index, and computes aligned results while the use case only coordinates inputs, persistence, and pipeline progress updates.
3. **Given** either pair generation or alignment completes or fails, **When** the orchestrating use case finishes its step, **Then** pipeline step status and metrics are recorded accurately.

---

### User Story 3 - Encapsulate Multi-Parameter Method Signatures into Cohesive Parameter Objects (Priority: P2)

As a developer working on the service, I want every inbound port, outbound port, use case, application service, and adapter operation to accept at most three individual parameters—grouping larger input sets into cohesive immutable parameter records—so that signatures are self-documenting, less error-prone, and compliant with project maintainability standards.

**Why this priority**: Operations accepting 4 to 6 positional parameters (especially multiple strings or booleans) are brittle and prone to argument-order mistakes. Encapsulating them into named parameter objects improves readability and enforces uniform contracts across layers.

**Independent Test**: Can be verified by inspecting all port, use case, service, and adapter signatures to confirm none exceed three parameters and by executing existing unit and architecture-governance tests to confirm all callers pass the encapsulated parameter objects cleanly.

**Acceptance Scenarios**:

1. **Given** any operation across inbound ports, outbound ports, use cases, application services, or outbound adapters previously accepting four or more parameters (such as pair-generation execution, knowledge-base generation, pipeline step updates, event lookups/upserts, object upserts, and alignment persistence/notifications), **When** refactored, **Then** every method accepts at most three parameters by grouping related fields into cohesive domain parameter objects.
2. **Given** pipeline step progress is reported with or without metrics, **When** callers construct a step-update parameter object, **Then** convenient factory constructors allow creating updates cleanly without passing four or five positional arguments.

---

### User Story 4 - Synchronize Use Case Documentation with Updated Contracts (Priority: P3)

As a team member, I want the use-case documentation to reflect the updated inbound port signatures and new application service dependencies, so that architectural documentation remains accurate and passes automated governance checks.

**Why this priority**: Outdated documentation misleads contributors and fails automated documentation governance checks. Keeping it synchronized guarantees long-term maintainability.

**Independent Test**: Can be verified by reviewing the updated use-case documentation files for accurate contracts and sequence flows and running automated documentation governance checks.

**Acceptance Scenarios**:

1. **Given** inbound port signatures or service dependencies change for pair generation, knowledge-base generation, alignment, or restriction-filtered event retrieval, **When** the corresponding use-case documentation files are updated, **Then** all contracts, flows, and diagrams match the implemented behavior and automated governance checks pass.

---

### Edge Cases

- What happens when a restriction filter value is `null`, blank, `"NO_RESTRICTED"`, or an unrecognized string? Unrecognized or invalid values should fail predictably or map cleanly according to defined restriction semantics (`null`, blank, and `"NO_RESTRICTED"` map to unrestricted behavior).
- What happens when a biological term has multiple underscores (e.g., `@GENE_BRCA_1`)? Only the first prefix separator after `@` delimits the biotype prefix from the remaining term suffix.
- What happens when parallel pair generation runs on an empty object family, single-object family, or duplicate synonyms across objects? Duplicate pairs are deduplicated safely without mutating shared unsynchronized state, and empty inputs return an empty set.
- What happens when an object or event upsert receives null or empty synonym/reference lists? The operation normalizes safely and defaults to empty collections without throwing null-pointer errors.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST define an explicit restriction level classification (`UNRESTRICTED`, `RESTRICTED`, `VERY_RESTRICTED`) with safe parsing from external string values (treating missing, blank, or `"NO_RESTRICTED"` values as unrestricted).
- **FR-002**: System MUST perform knowledge-base event restriction filtering in the application layer rather than inside persistence adapters, leaving storage adapters responsible strictly for data access and persistence mapping.
- **FR-003**: System MUST centralize biological term prefix/suffix normalization (`@PREFIX_SUFFIX`) in a single stateless domain utility used consistently by both event and object persistence flows, eliminating duplicated parsing logic.
- **FR-004**: System MUST extract publication pair-generation logic (level-1 and higher-level term pairing) from the pair-building use case into a dedicated application service interface and implementation.
- **FR-005**: System MUST eliminate concurrent mutation of non-thread-safe collections during parallel pair generation so that concurrent execution produces complete, deterministic results without race conditions.
- **FR-006**: System MUST extract expert-object alignment logic (symbol normalization, inverted-synonym indexing, and alignment computation) from the alignment use case into a dedicated application service interface and implementation.
- **FR-007**: System MUST encapsulate all parameter lists exceeding three individual arguments across inbound ports, outbound ports, use cases, application services, and outbound adapters into cohesive, immutable domain parameter objects.
- **FR-008**: System MUST preserve existing external HTTP endpoint schemas and response contracts on public controllers.
- **FR-009**: System MUST update all affected use-case documentation files to reflect modified inbound port contracts and new application service collaborations.
- **FR-010**: System MUST pass all compilation, unit test, architecture-layer governance, and documentation governance checks after the refactoring.

### Key Entities

- **Restriction Level**: Represents the access/visibility classification applied when filtering knowledge-base events (`UNRESTRICTED`, `RESTRICTED`, `VERY_RESTRICTED`).
- **Normalized Biological Term**: Represents the parsed outcome of a raw biological identifier, separating an optional biotype prefix from the normalized term suffix.
- **Pair Generation Command**: Encapsulates all inputs required to trigger publication pair generation (pipeline identifier, short-name preference, hierarchy levels, and requesting user identifier).
- **Pipeline Step Update**: Encapsulates pipeline step progress reporting details (pipeline identifier, step identifier, status, optional message, and optional metrics).
- **Knowledge-Base Event Key & Object Upsert Command**: Encapsulate multi-field lookup keys and upsert payloads for knowledge-base events and biological objects.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of methods across inbound ports, outbound ports, use cases, application services, and adapters accept 3 or fewer parameters (zero methods with 4+ parameters).
- **SC-002**: 100% of biological term prefix/suffix normalization logic is consolidated into a single shared domain utility (zero duplicated regex/parsing blocks across adapters).
- **SC-003**: Parallel pair generation executes across concurrent object families with zero concurrency exceptions and 100% deterministic pair output compared to sequential execution.
- **SC-004**: Persistence adapters contain zero restriction-level business branching.
- **SC-005**: 100% of unit tests, architecture-layer governance checks, and documentation governance checks pass after completion with zero regressions.

## Assumptions

- External HTTP request/response schemas and RabbitMQ message payload schemas remain unchanged; parameter encapsulation applies to internal port, service, use-case, and adapter boundaries.
- Moving restriction filtering into the application layer using existing pipeline-level event retrieval meets current performance expectations; database-level aggregation pushdown is out of scope for this change.
- Existing automated architecture and documentation governance suites define the authoritative rules for layer boundaries, naming conventions, and documentation completeness.

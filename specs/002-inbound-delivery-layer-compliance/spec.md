# Feature Specification: Inbound Consumers, REST Controller and Lifecycle Delivery-Layer Compliance

**Feature Branch**: `002-inbound-delivery-layer-compliance`

**Created**: 2026-11-24

**Status**: Draft

**Input**: User description: "/speckit-specify basado en /home/yacson/Proyectos/biopatternsg-ms/SDD/pubmed-integrations/02-inbound-consumers-and-controller-delivery-layer-compliance"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Delegate Message Consumer Workflows to Dedicated Use Cases (Priority: P1)

As a service architect and maintainer, I need the NCBI and PubTator queue consumers to act solely as thin message-delivery adapters that delegate to dedicated inbound business ports so that search orchestration, caching, progress tracking, and metrics reporting are encapsulated in testable use cases.

**Why this priority**: Currently, both message consumers bypass inbound business ports and embed full pipeline orchestration and 5–6 outbound dependencies directly in the delivery layer, violating hexagonal isolation and preventing independent unit testing of the search workflows.

**Independent Test**: Can be tested independently by verifying that each queue consumer depends exclusively on its single inbound port, that external PubTator search parsing is encapsulated behind its outbound adapter, and that unit tests for both new use cases plus architecture/documentation governance checks pass.

**Acceptance Scenarios**:

1. **Given** an incoming NCBI search message on the queue, **When** the NCBI consumer receives and deserializes the message, **Then** it delegates execution to a dedicated inbound use case that performs the search, persists results, tracks atomic completion progress, cleans up processed pairs, and reports step completion metrics.
2. **Given** an incoming PubTator batch message on the queue, **When** the PubTator consumer receives and deserializes the message, **Then** it delegates execution to a dedicated inbound use case that filters uncached identifiers, retrieves parsed domain results from the PubTator outbound port, filters relevant events, persists results, and reports step completion metrics.
3. **Given** the two new use cases, **When** documentation and unit tests are added, **Then** both use cases have full unit test coverage and pass all automated documentation governance checks.

---

### User Story 2 - Clean Up REST Controller Naming, Async Dispatch, and Request Validation (Priority: P1)

As an API maintainer, I need the REST controller to use accurate handler names, a single reusable asynchronous dispatch helper, and declarative request validation so that delivery endpoints are consistent, concise, and free of duplicated boilerplate.

**Why this priority**: The REST controller currently contains a copy-pasted handler name from another service, five duplicated asynchronous execution blocks, and inconsistent manual vs. declarative parameter validation that risks missing validation on request payloads.

**Independent Test**: Can be tested independently by verifying that the pairs-building endpoint handler is accurately named, asynchronous endpoints share a single dispatch helper, declarative validation is applied to path/body parameters while preserving existing HTTP status codes (`202`, `200`, `400`, `404`), and all tests pass.

**Acceptance Scenarios**:

1. **Given** the pairs-building endpoint, **When** inspecting the controller handler, **Then** it uses a domain-accurate method name (`buildPubmedPairs`) instead of the legacy misnamed handler.
2. **Given** the five asynchronous `202 Accepted` pipeline endpoints, **When** dispatching background work, **Then** all five endpoints delegate to a single shared private helper rather than duplicating error-catching async blocks.
3. **Given** REST endpoints accepting path parameters or request payloads (including restriction queries and role updates), **When** invalid or missing inputs are submitted, **Then** declarative validation enforces non-blank and valid constraints while preserving existing HTTP status codes.

---

### User Story 3 - Harden Session Context Extraction and Startup Lifecycle Resumption (Priority: P2)

As a service operator, I need user-context extraction to handle missing headers safely without runtime crashes and startup pipeline resumption to release network connections and log accurate timing information.

**Why this priority**: Unchecked header access can trigger null-pointer or missing-element exceptions on requests without a user header, and unclosed HTTP responses during startup resumption leak connections while inaccurate log messages mislead operators.

**Independent Test**: Can be tested independently by unit-testing user-context extraction with null context and missing/empty user headers, and verifying startup lifecycle resumption closes its HTTP response safely and logs the actual configured delay.

**Acceptance Scenarios**:

1. **Given** an incoming request where the session context is null or the user identifier header is missing or empty, **When** the user identifier is requested, **Then** the session helper returns safely (e.g., `null`) without throwing an exception.
2. **Given** application startup scheduling pipeline resumption, **When** the scheduled resumption runs, **Then** the startup component logs the actual configured delay in seconds, uses the standard project logger, and closes the HTTP response reliably after checking its status.

---

### Edge Cases

- What happens when a queue message arrives with missing or malformed fields? The consumer must handle parsing/execution errors safely without crashing the consumer loop, while preserving progress tracking guarantees.
- What happens when `PubtatorSearchRepoWeb.search(...)` receives an empty list of identifiers or returns zero documents with events? The use case must handle empty lists gracefully without persisting empty records and still report batch completion progress.
- What happens when a REST request omits the `x-user-id` header? `SessionUtils.getUserId()` must return `null` cleanly rather than throwing `NullPointerException` or `NoSuchElementException`.
- What happens when `configAndControlHttpClient.resumePipelines(...)` returns an error status or throws an exception during startup? The `Response` must still be closed deterministically via `try-with-resources`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST extract NCBI queue message processing into a dedicated inbound port (`ProcessNcbiSearchRequest`) and use case (`ProcessNcbiSearchRequestUseCase`) owning NCBI search execution, PMID persistence, atomic progress increment/completion check, pair cleanup, and `SEARCH_PUBMED_IDS` step notification.
- **FR-002**: System MUST extract PubTator batch processing into a dedicated inbound port (`ProcessPubtatorBatch`) and use case (`ProcessPubtatorBatchUseCase`) owning uncached PMID filtering, PubTator retrieval, event filtering, result persistence, atomic progress increment/completion check, and `SEARCH_PUBTATOR` step notification.
- **FR-003**: System MUST move PubTator response parsing behind the `PubtatorSearchRepoWeb` outbound port / `PubtatorSearchAdapter` so the port returns parsed domain results (`List<PubtatorResult>`) rather than raw JSON strings.
- **FR-004**: System MUST refactor `NcbiRabbitConsumer` and `PubtatorRabbitConsumer` so each injects ONLY its corresponding inbound port and zero outbound repositories or response parsers.
- **FR-005**: System MUST provide unit tests and English use case documentation (indexed in `.agents/use_cases/README.md` and Section 5 of `.agents/architecture.md`) for both `ProcessNcbiSearchRequestUseCase` and `ProcessPubtatorBatchUseCase`.
- **FR-006**: System MUST rename `buildTreeMesh` to `buildPubmedPairs` in `PubmedController`.
- **FR-007**: System MUST consolidate the five duplicated asynchronous `CompletableFuture.runAsync` blocks in `PubmedController` into a single private helper method.
- **FR-008**: System MUST apply declarative Jakarta Bean Validation (`@NotBlank`, `@Valid`) to `PubmedController` parameters and request bodies (including `KbEventsByRestrictionRequest` and `rolesMap` / role-update endpoints) while preserving existing HTTP status codes (`202`, `200`, `400`, `404`).
- **FR-009**: System MUST make `SessionUtils.getUserId()` null-safe so it returns `null` when `context` is null or `x-user-id` is missing or empty, verified by unit tests.
- **FR-010**: System MUST refactor `AppLifecycleBean` to use SLF4J (`@Slf4j`), log the actual `DEFAULT_DELAY_SECONDS` value dynamically, and close the JAX-RS `Response` from `resumePipelines(...)` using `try-with-resources`.

### Key Entities

- **NCBI Search Request**: Represents a queued term-search job (`pipelineId`, `term`, `retmax`, `pairIndex`, `totalPairs`, `userId`) processed by `ProcessNcbiSearchRequestUseCase`.
- **PubTator Search Request**: Represents a queued batch of PMIDs (`pipelineId`, `pmids`, `batchIndex`, `totalBatches`, `userId`) processed by `ProcessPubtatorBatchUseCase`.
- **PubTator Result**: Domain representation of an annotated publication returned by `PubtatorSearchRepoWeb`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Both queue consumers (`NcbiRabbitConsumer` and `PubtatorRabbitConsumer`) inject exactly 1 inbound port and 0 outbound repositories or parsers.
- **SC-002**: 100% of new use cases (2 of 2) have passing unit tests, English use case documentation, and index entries passing automated documentation governance checks.
- **SC-003**: `PubmedController` has 0 occurrences of `buildTreeMesh`, 0 duplicated multi-line async dispatch blocks (reduced from 5 to 1 helper), and declarative validation on all applicable endpoints.
- **SC-004**: `SessionUtils.getUserId()` handles null context and missing/empty headers with 0 exceptions in unit tests.
- **SC-005**: `AppLifecycleBean` releases 100% of startup HTTP `Response` objects via `try-with-resources` and logs the exact configured delay.
- **SC-006**: 100% of compilation, architectural boundary checks, documentation governance checks, and unit tests pass with zero errors.

## Assumptions

- Existing RabbitMQ exchange names, queue names, channel names (`ncbi-in`, `ncbi-out`, `pubtator-in`, `pubtator-out`), and `@Blocking(ordered = false)` concurrency settings remain unchanged.
- Existing REST route paths and JSON request/response payload schemas on `/pubmed/*` remain backward-compatible.
- `SessionUtils.getUserId()` returning `null` on missing/empty `x-user-id` is compatible with downstream use cases that accept nullable `userId`.

# Phase 0 Research: Inbound Consumers, REST Controller and Lifecycle Delivery-Layer Compliance

**Feature**: `002-inbound-delivery-layer-compliance` | **Date**: 2026-11-24

## 1. Moving PubTator JSON Parsing Behind `PubtatorSearchRepoWeb`

- **Decision**: Change `PubtatorSearchRepoWeb.search(List<String> pmids)` return type from `String` to `List<PubtatorResult>`, and inject `PubtatorResponseParser` into `PubtatorSearchAdapter` so JSON parsing happens inside the outbound adapter.
- **Rationale**: `ProcessPubtatorBatchUseCase` belongs to the `application` layer and must operate on pure `domain.model.PubtatorResult` objects rather than raw PubTator BioCJSON strings or infrastructure parser classes.
- **Alternatives Considered**:
  - Keeping `PubtatorResponseParser` in `application`: Rejected because BioCJSON parsing and `PubtatorResultMapper` are infrastructure concerns.

## 2. Dedicated Inbound Ports & Use Cases for RabbitMQ Consumers

- **Decision**:
  - Create `com.biopatternsg.domain.ports.in.ProcessNcbiSearchRequest` (`void execute(NcbiSearchRequest request)`) implemented by `ProcessNcbiSearchRequestUseCase`.
  - Create `com.biopatternsg.domain.ports.in.ProcessPubtatorBatch` (`void execute(PubtatorSearchRequest request)`) implemented by `ProcessPubtatorBatchUseCase`.
  - Refactor `NcbiRabbitConsumer` and `PubtatorRabbitConsumer` to inject only their single respective Inbound Port, deserialize `JsonObject`, and delegate.
- **Rationale**: Enforces Constitution Principles II and III (consumers must not inject outbound repositories or embed business orchestration) while keeping RabbitMQ channel/concurrency annotations (`@Incoming`, `@Blocking(ordered = false)`) unchanged.
- **Alternatives Considered**:
  - Leaving orchestration in consumers with private helpers: Rejected as a direct violation of Constitution Principles II & III.

## 3. `PubmedController` Async Helper & Declarative Validation

- **Decision**:
  - Rename `buildTreeMesh` to `buildPubmedPairs`.
  - Extract `private void runAsyncTask(String taskName, Runnable action)` wrapping `CompletableFuture.runAsync(() -> { try { action.run(); } catch (Exception e) { log.error("Error {}", taskName, e); } }, executor);` and reuse it across all 5 `202 Accepted` endpoints.
  - Replace manual `isBlank()` checks on `@PathParam("name")`, `@PathParam("term")`, and `@PathParam("pipelineId")` with Jakarta `@NotBlank`, add `@NotNull` on `rolesMap` in `updateKbObjectRoles`, and add `@Valid` on `KbEventsByRestrictionRequest` in `getKbEventsByRestriction`.
- **Rationale**: Eliminates 5 duplicated async try/catch blocks and standardizes input validation with Hibernate Validator while preserving existing HTTP status codes (`202`, `200`, `400`, `404`).
- **Alternatives Considered**:
  - Keeping manual `if (param == null || param.isBlank())` checks: Rejected because declarative Jakarta Bean Validation is mandated by `REQ-PUBMED-ARCH-02` and reduces controller boilerplate.

## 4. `SessionUtils` Null-Safety & `AppLifecycleBean` Resource Safety

- **Decision**:
  - Update `SessionUtils.getUserId()` to check `this.context == null`, `List<String> values = this.context.get("x-user-id")`, and `values == null || values.isEmpty()`, returning `null` safely.
  - Update `AppLifecycleBean` to use Lombok `@Slf4j` (removing `org.jboss.logging.Logger`), log `DEFAULT_DELAY_SECONDS` dynamically (`"in {} seconds"`), and wrap `configAndControlHttpClient.resumePipelines(...)` in `try (Response response = ...)` logging `response.getStatus()`.
- **Rationale**: Prevents `NullPointerException` / `NoSuchElementException` when `x-user-id` is omitted, prevents JAX-RS `Response` connection leaks on startup, and aligns logging with project standards.
- **Alternatives Considered**:
  - Returning `Optional<String>` from `SessionUtils.getUserId()`: Rejected to avoid breaking existing callers (`PubmedController` passes `String userId` directly to Inbound Ports).

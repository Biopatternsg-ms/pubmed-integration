<!--
Sync Impact Report
- Version change: template → 1.0.0
- Added principles:
  - I. Hexagonal Layer Isolation (NON-NEGOTIABLE)
  - II. Use Cases & Application Services Encapsulation
  - III. Zero Business Logic in Inbound & Outbound Adapters
  - IV. Java 21 Clean Code & Method Discipline
  - V. Stateless Pure Mappers, Naming Conventions & Use Case Documentation
- Added sections:
  - Technology & Integration Constraints
  - Development Workflow & Quality Gates
- Removed sections: None
- Follow-up TODOs: None
-->

# pubmed-integration Constitution

## Core Principles

### I. Hexagonal Layer Isolation (NON-NEGOTIABLE)

The service MUST maintain strict Hexagonal Architecture (Ports & Adapters) boundaries across `com.biopatternsg.domain.*`, `com.biopatternsg.application.*`, and `com.biopatternsg.infrastructure.*`:
- **Domain (`domain.*`):** MUST remain a pure business core (`model`, `ports.in`, `ports.out`, `util` including `LogSanitizer`) and MUST NOT depend on `application..`, `infrastructure..`, or delivery/persistence/messaging frameworks (e.g., `jakarta.ws.rs.*`, MongoDB Panache, RabbitMQ/Vert.x).
- **Application (`application.*`):** MUST depend only on `domain..` and MUST NOT depend on `infrastructure..`. All new or refactored Use Cases and Outbound Ports MUST accept and return pure Domain Models (`domain.model.*`) rather than legacy `com.biopatternsg.mongo.*` collections.
- **Infrastructure (`infrastructure.*`):** MUST interact with Application exclusively through Inbound Ports (`domain.ports.in.*`); direct access to `application..` or `application.usecase..` from outside Application is prohibited.

### II. Use Cases & Application Services Encapsulation

Every business interaction MUST enter through a dedicated Inbound Port (`domain.ports.in.*`) implemented by a single-responsibility `*UseCase` in `application.usecase.*`:
- Use Cases MUST act purely as lightweight orchestrators: validate preconditions and input guards (e.g., blank parameter checks, numeric PMID filtering/deduplication, `PubtatorResult.hasKnowledgeBase()` caching checks), coordinate Outbound Ports (`domain.ports.out.*`) or Application Services (`application.services.*`), and report pipeline step progress and metrics via `ConfigAndControlRepository`.
- Reusable multi-step biological lookups, pair-building strategies, alignment algorithms, and cross-service query coordination MUST reside in Application Services (`application.services.*` interfaces with `@ApplicationScoped` implementations in `application.services.impl.*`, e.g., `BiologicalObjectsService`, never exposing internal helper methods publicly).
- Application Services MUST NEVER be injected directly into REST Controllers or RabbitMQ Consumers; all inbound adapters MUST depend strictly on Inbound Port interfaces.

### III. Zero Business Logic in Inbound & Outbound Adapters

Delivery, messaging, and persistence adapters MUST remain thin and free of domain rules:
- **REST Controllers (`infrastructure.adapters.in.restcontrollers.*`):** Controllers (`PubmedController`, `@Path("/pubmed")`) MUST only receive and validate HTTP requests (`@Valid` DTOs in `infrastructure.adapters.dtos.*`), extract `userId` from request-scoped `SessionUtils` (populated by `ContextInterceptor` from `x-user-id`) before async dispatch, invoke Inbound Ports (synchronously or via `@ActivateRequestContext` + managed `Executor` + `CompletableFuture.runAsync(...)` for `202 Accepted` pipeline endpoints), and return HTTP responses. Business branching, pair generation, alignment calculations, and direct repository queries are prohibited in controllers.
- **Messaging Consumers (`infrastructure.adapters.in.consumers.*`):** RabbitMQ consumers (`NcbiRabbitConsumer` on `@Incoming("ncbi-in")`, `PubtatorRabbitConsumer` on `@Incoming("pubtator-in")`, `@Blocking(ordered = false)`) MUST deserialize `JsonObject` queue payloads into Domain Models (`NcbiSearchRequest`, `PubtatorSearchRequest`), handle transport/parsing errors safely with guaranteed progress tracking (`finally`), and—for new or refactored consumers—delegate business execution exclusively to dedicated Inbound Ports (`domain.ports.in.*`).
- **Outbound Adapters (`infrastructure.adapters.out.*`):** Repository adapters (`PanacheMongoRepository`), messaging producer adapters (`@Channel("ncbi-out")`, `@Channel("pubtator-out")`), and external/internal service adapters MUST implement `domain.ports.out.*` interfaces and—for all new or refactored adapters—convert MongoDB collections (`com.biopatternsg.mongo.*`) or client DTOs into Domain Models (`domain.model.*`) before returning to Application.

### IV. Java 21 Clean Code & Method Discipline

All Java code in `pubmed-integration` MUST adhere to clean, low-complexity standards:
- **Maximum 3 Parameters:** No method in a Use Case, Application Service, or Adapter may accept more than 3 individual parameters; larger inputs MUST be encapsulated in a Java `record`, Domain Model, or Command/Query object.
- **Streams & Private Helpers:** Complex multi-step logic MUST be decomposed into small single-responsibility private methods. Stream/Optional lambdas MUST be single-expression calls or method references and MUST NOT embed multi-line builders, branching, or try/catch blocks. Prefer `.toList()`, Java `record` Domain Models (`domain.model.*`) and DTOs (`infrastructure.adapters.dtos.*`, `infrastructure.clients.dtos.*`), and enhanced `switch` expressions for exhaustive enum handling (`PipelineSteps`, `Status`).
- **Injection, Null Safety & Exceptions:** Dependencies MUST prefer constructor injection (`private final` fields with `@RequiredArgsConstructor` or an explicit constructor with `@Inject` / `@RestClient` / `@ConfigProperty`). Use `Objects.equals(a, b)`, return empty collections/maps (`List.of()`, `Collections.emptyList()`, `Collections.emptyMap()`) instead of `null`, avoid dead statements, close JAX-RS `Response` objects with `try-with-resources`, and use `quarkus-exceptions-handler` (`com.cifertech.exceptionhandler.exceptions.*`) for standardized HTTP error mapping.
- **Explicit `toString()`:** Every class in `domain.model.*`, `infrastructure.adapters.dtos.*`, and `infrastructure.clients.dtos.*` MUST define an explicit `toString()` (automatically as a Java `record` or via `@ToString` / `@Data`), excluding sensitive fields (e.g., API keys, tokens, secrets) with `@ToString.Exclude`.
- **Log Injection Prevention (CWE-117 / CodeQL):** Every user-provided, message-queue, or external untrusted value passed to logger calls (`log.info`, `log.warn`, `log.error`, `log.debug`) MUST be sanitized via `com.biopatternsg.domain.util.LogSanitizer.sanitize(value)` (replacing `\n` and `\r` with `_`) before logging. Logging raw unsanitized external inputs is strictly prohibited.

### V. Stateless Pure Mappers, Naming Conventions & Use Case Documentation

Mapping, structural naming, and documentation standards MUST be strictly enforced:
- **Static Mappers:** New or refactored mapper classes in `infrastructure.mapper.*` MUST be `public final` utility classes with private constructors, `public static` deterministic pure functions, zero mutable state, zero CDI annotations (`@ApplicationScoped`, `@Singleton`, `@RequestScoped`), and null-safe handling using standardized method names (`requestToModel`, `collectionToModel`, `modelToCollection`, `modelToResponse`, `<provider>ToModel`).
- **ArchUnit Naming Conventions:** Classes in `application.usecase..` MUST end with `UseCase`, classes in `infrastructure.adapters.in.restcontrollers..` MUST end with `Controller`, and classes in `infrastructure.adapters.out..` MUST end with `Adapter`.
- **Use Case Documentation:** Every new or modified Use Case MUST have English documentation in `.agents/use_cases/<UseCaseName>.md` (problem, contracts, step-by-step flow, Mermaid `sequenceDiagram`, and caching/failure scenarios) and MUST be indexed in both `.agents/use_cases/README.md` and Section 5 of `.agents/architecture.md`.

## Technology & Integration Constraints

- **Stack:** Java 21, Quarkus 3.31.4, Lombok, Jakarta Bean Validation, `quarkus-exceptions-handler`, ArchUnit, MongoDB with Panache (`com.biopatternsg.mongo.*`: `pairs`, `pubmed_results`, `search_progress`, `pubtator_results`, `pubtator_search_progress`, `kb_events`, `kb_objects`, `alignedResults`), and SmallRye Reactive Messaging RabbitMQ (`exchange-ncbi-pubmed` / `ncbi-pubmed-queue` on `ncbi-out` & `ncbi-in`; `exchange-pubtator` / `pubtator-queue` on `pubtator-out` & `pubtator-in`).
- **Internal Microservice Integrations:** Consumed via MicroProfile REST Clients and outbound adapters for `search-biological-objects` (`biological-objects-api` via `BiologicalObjectsRepositoryAdapter`), `config-and-control` (`config-and-control-api` via `ConfigAndControlRepositoryAdapter` and startup pipeline resumption in `AppLifecycleBean` for `PUBMED_INTEGRATION`), and `build-knowledge-base` (`build-knowledge-base-api` via `BuildKnowledgeBaseAdapter`).
- **External Biological APIs:** Consumed via MicroProfile REST Clients, rate-limited wrappers (`SimpleRateLimiter`), and outbound adapters for NCBI E-utilities ESearch (`ncbi-esearch-api`, `POST /esearch.fcgi`, rate-limited by `ncbi.rate-limit.with-key` / `without-key` via `NcbiSearchAdapter`) and PubTator3 (`pubtator-api`, `GET /publications/export/biocjson`, batched by `pubtator.batch-size` and rate-limited by `pubtator.rate-limit` via `PubtatorSearchAdapter`).
- **Licensing:** Source files MUST maintain Apache 2.0 license headers as configured in `pom.xml` (`license-maven-plugin`).

## Development Workflow & Quality Gates

Before completing any feature, refactoring, or bug fix:
1. Run `./mvnw clean compile` (using Java 21) to verify compilation and refresh `target/classes`.
2. Run `./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest` to verify all ArchUnit hexagonal layer/naming rules and Use Case documentation governance checks pass.
3. Run `./mvnw test` to confirm all unit tests pass without regressions.
4. Update `.agents/use_cases/<UseCaseName>.md`, `.agents/use_cases/README.md`, and `.agents/architecture.md` synchronously whenever Use Cases, Ports, or integrations are added or modified.

## Governance

This Constitution supersedes ad-hoc development practices in `pubmed-integration`. Detailed architectural and coding standards are maintained in `.agents/architecture-rules.md`, `.agents/java-rules.md`, `.agents/architecture.md`, and `.agents/use_cases/README.md`.

- **Amendment Procedure:** Proposed changes to governance principles MUST update this constitution, include a Sync Impact Report for review, and synchronize affected `.agents/*.md` guidance documents.
- **Versioning:** Amendments follow semantic versioning (`MAJOR` for backward-incompatible principle removals or redefinitions, `MINOR` for new principles or materially expanded guidance, `PATCH` for clarifications and non-semantic refinements).
- **Compliance:** All pull requests, Spec Kit plans (`Constitution Check`), and AI-assisted implementations MUST verify compliance with these principles and pass `ArchitectureLayerTest` and `DocumentationGovernanceTest`.

**Version**: 1.0.0 | **Ratified**: 2026-11-23 | **Last Amended**: 2026-11-23

# Architectural Rules and Design Standards

> **Service:** `pubmed-integration`  
> **Package Base:** `com.biopatternsg.*`  
> **Framework:** Quarkus 3.31.4 (Java 21) | MongoDB Panache | SmallRye Reactive Messaging (RabbitMQ)  
> **Architecture:** Hexagonal (Ports & Adapters) / Clean Architecture

This document establishes the mandatory **architectural rules, layer boundaries, and component responsibilities** that developers and AI agents must follow when creating or modifying features in the `pubmed-integration` service.

*(For Java language, clean code, lambdas, and static mapper coding rules, see [`java-rules.md`](java-rules.md)).*

---

## 1. Hexagonal Layer Boundaries & Isolation

1. **Domain Layer (`com.biopatternsg.domain.*`):**
    - Pure business core (`model`, `ports.in`, `ports.out`, `util`).
    - **MUST NOT** depend on `application..`, `infrastructure..`, or external delivery/persistence frameworks (e.g., `jakarta.ws.rs.*`, MongoDB Panache, RabbitMQ/Vert.x).
    - Cross-cutting pure domain utilities such as `com.biopatternsg.domain.util.LogSanitizer` must remain framework-free.
2. **Application Layer (`com.biopatternsg.application.*`):**
    - Contains Use Cases (`application.usecase.*`), Application Services (`application.services.*`, `application.services.impl.*`), and application utilities (`application.util.*`, e.g., `PairsGenerator`).
    - **MUST** depend only on `domain..` and **MUST NOT** depend on `infrastructure..`.
    - Can only be accessed from outer layers through Inbound Ports (`domain.ports.in.*`).
    - *Legacy Persistence Boundary Note:* MongoDB Panache entities currently reside in `com.biopatternsg.infrastructure.mongo.*` and some legacy repository ports/use cases reference them directly. For all **new or refactored** Use Cases and Outbound Ports, strictly return and accept pure Domain Models (`domain.model.*`) rather than `com.biopatternsg.infrastructure.mongo.*` collections.
3. **Infrastructure Layer (`com.biopatternsg.infrastructure.*` & `com.biopatternsg.infrastructure.mongo.*`):**
    - Contains inbound delivery adapters (`adapters.in.restcontrollers.*`, `adapters.in.consumers.*`), outbound persistence, messaging, and external/internal adapters (`adapters.out.*`, `adapters.out.external_repositories.*`, `adapters.out.producers.*`), request/response DTOs (`adapters.dtos.*`, `clients.dtos.*`), MicroProfile HTTP clients (`clients.external.*`, `clients.internal.*`), service wrappers (`external_services.*`, `internal_services.*`), mappers (`mapper.*`), lifecycle hooks (`boot_functions.*`), request interceptors/session context (`config.interceptors.*`, `session.*`), infrastructure utilities (`util.SimpleRateLimiter`), and MongoDB Panache collections (`com.biopatternsg.infrastructure.mongo.*`).

---

## 2. Use Cases (`com.biopatternsg.application.usecase`)

### 2.1. Single Entry-Point Responsibility
- Each Use Case represents a single business interaction and implements its dedicated Inbound Port interface defined under `com.biopatternsg.domain.ports.in.*` (e.g., `BuildPubmedPairs`, `SearchPubmedByPairs`, `SearchPubtatorByPmids`, `GenerateKbForPipeline`, `GenerateAlignedObjects`, `GetKbEventsByRestriction`, `GetPublicationsByPmids`).
- Use Cases must implement only the contract methods defined by their inbound port interface (typically `execute(...)`).
- Concrete Use Case classes (`*UseCase.java`) must never be directly injected or instantiated across layers; callers (REST controllers and message consumers) must always depend on the Inbound Port interface.

### 2.2. Use Cases as Lightweight Orchestrators
Implementations (`*UseCase.java`) must remain lean and act purely as orchestrators:
- Validate input preconditions and business constraints (e.g., null/blank `pipelineId`, `term`, or `name` guards, numeric PMID filtering/deduplication, caching checks on `PubtatorResult.hasKnowledgeBase()`).
- Coordinate execution by invoking outbound ports (`domain.ports.out.repositories.*`, `domain.ports.out.external_repositories.*`, `domain.ports.out.producers.*`) or application services (`application.services.*`).
- Report pipeline step progress, completion metrics, and failure states through `ConfigAndControlRepository` (`PipelineSteps`, `Status`) for pipeline orchestration steps (`COMBINATIONS`, `BUILD_KNOWLEDGE_BASE`, `GENERATE_ALIGNED_OBJECTS`).
- Delegate cross-service hierarchy queries and reusable multi-step strategies to **Application Services** (`BiologicalObjectsService`).

### 2.3. Mandatory Use Case Documentation (`.agents/use_cases/`)
- **Mandatory English Language:** All Use Case documentation files under `.agents/use_cases/` (including `README.md` and every `<UseCaseName>.md` file) **must be written strictly in English**.
- **New Use Cases:** Whenever a new Use Case is created, a corresponding markdown document must be created in English under `.agents/use_cases/<UseCaseName>.md` describing:
    1. Business problem and purpose.
    2. Input and output contracts (Inbound port, Outbound ports, Application Services).
    3. Step-by-step execution flow.
    4. Sequence diagram using Mermaid format (`sequenceDiagram`).
    5. Edge cases, caching/idempotency behavior, and validation failure scenarios.
- **Modifications:** Whenever an existing Use Case signature, flow, or rule is modified, its documentation in `.agents/use_cases/<UseCaseName>.md` must be updated synchronously in English (creating the file if it does not yet exist).
- **Mandatory Index References (`README.md` & `architecture.md`):** Whenever a new Use Case documentation file is added (or an existing one is renamed/repurposed), its reference and link **must** be added/updated in both:
    1. `.agents/use_cases/README.md` (the Use Cases catalog index).
    2. Section 5 of `.agents/architecture.md`.

---

## 3. Application Services (`com.biopatternsg.application.services`)

### 3.1. Encapsulating Complex Business Logic and Strategies
- Reusable multi-step biological lookups, pair-building strategies, alignment algorithms, and cross-service query coordination belong in **Application Services**.
- Current example in `pubmed-integration`:
    - `BiologicalObjectsService` (`BiologicalObjectsServiceImpl`): Coordinates level-based and hierarchy-based (`father`, `brothers`, `sons`) queries against `search-biological-objects` via `BiologicalObjectsRepository`.

### 3.2. Strict Encapsulation and Interface-Implementation Separation
For any new or refactored Application Service:
1. **Interface Contract:** The public contract must be defined as an interface under `com.biopatternsg.application.services.*`.
2. **Hidden Implementation:** The implementation class must reside in `com.biopatternsg.application.services.impl.*` annotated with `@ApplicationScoped`.
3. **Encapsulation:** Concrete implementation classes must not expose internal helper methods publicly. Only the interface contract methods should be visible outside the package.
4. **CDI Injection:** Always inject the **interface** into Use Cases or other Application Services, never the concrete implementation class.

### 3.3. Service Injection and Dependencies
- Application Services may coordinate multiple Outbound Ports (`domain.ports.out.repositories.*`, `domain.ports.out.external_repositories.*`, and `domain.ports.out.producers.*`).
- **Prohibition:** Application Services must **never** be injected directly into REST Controllers or RabbitMQ Consumers; all external traffic must enter via Inbound Ports (`domain.ports.in.*`) implemented by Use Cases.

---

## 4. Inbound Adapters (REST Controllers, Messaging Consumers & Lifecycle)

### 4.1. Zero Business Logic in REST Controllers (`infrastructure.adapters.in.restcontrollers.*`)
- Controllers (`PubmedController`, `@Path("/pubmed")`) are strictly HTTP delivery adapters:
    1. Receive and validate HTTP requests (`@Valid`) using DTOs in `com.biopatternsg.infrastructure.adapters.dtos.*`.
    2. Extract request-scoped user context (`sessionUtils.getUserId()`, populated by `ContextInterceptor` from the `x-user-id` header) prior to launching async background tasks.
    3. Invoke the Use Case through its Inbound Port interface (`domain.ports.in.*`) — either synchronously (`200 OK` / `404 Not Found` / `400 Bad Request`) for KB/alignment/publication queries and role updates, or via `@ActivateRequestContext` + managed `Executor` + `CompletableFuture.runAsync(...)` for asynchronous `202 Accepted` pipeline endpoints (`/build-pairs`, `/search-pubmed-ids-by-pairs`, `/search-pubtator-by-pmids`, `/generate-kb`, `/generate-aligned-objects`).
    4. Return the appropriate HTTP `Response`.
- **No** business branching, pair generation, alignment calculations, or direct repository queries are allowed inside controller methods.

### 4.2. Messaging Consumers (`infrastructure.adapters.in.consumers.*`)
- RabbitMQ consumers (`NcbiRabbitConsumer` on `@Incoming("ncbi-in")` and `PubtatorRabbitConsumer` on `@Incoming("pubtator-in")`, both `@Blocking(ordered = false)`) are inbound delivery adapters:
    1. Deserialize incoming `JsonObject` queue payloads into Domain Models (`NcbiSearchRequest`, `PubtatorSearchRequest`).
    2. For new or refactored consumers, delegate processing to a dedicated Inbound Port (`domain.ports.in.*`) rather than orchestrating multiple outbound repositories directly inside the consumer class.
    3. Handle transport, parsing (`PubtatorResponseParser`), and downstream errors safely with structured logging and guaranteed progress tracking (`finally`) so pipeline completion is never deadlocked.

### 4.3. Startup Lifecycle & Request Context
- `AppLifecycleBean` (`infrastructure.boot_functions`) observes `StartupEvent` and triggers a delayed asynchronous pipeline resumption call to `config-and-control` for `PUBMED_INTEGRATION`.
- `ContextInterceptor` (`infrastructure.config.interceptors`) captures incoming JAX-RS headers into request-scoped `SessionUtils` (`infrastructure.session`). Always read `userId` on the request thread before handing work off to `Executor` threads.

### 4.4. Validation and Business Exceptions in Domain/Application Layers
- Structural request validation uses Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, `@NotEmpty`) on request DTOs.
- Use `quarkus-exceptions-handler` (`com.cifertech.exceptionhandler.exceptions.*`) for standardized HTTP error mapping when throwing technical or domain exceptions.
- Existence checks, deduplication, caching decisions, and alignment logic belong in Use Cases or Application Services — never in controllers or consumers.

---

## 5. Persistence, Messaging & Outbound Adapters (`com.biopatternsg.infrastructure.adapters.out.*`)

### 5.1. Repository Adapters (`infrastructure.adapters.out.*`)
- Implement repository interfaces defined in `com.biopatternsg.domain.ports.out.repositories.*` (`PairRepository`, `PubmedResultRepository`, `SearchProgressRepository`, `PubtatorPmidReaderRepository`, `PubtatorResultRepository`, `PubtatorSearchProgressRepository`, `KbEventRepository`, `KbObjectRepository`, `AlignedResultRepository`) using `PanacheMongoRepository<*>`.
- Separate concerns: MongoDB collections (`com.biopatternsg.infrastructure.mongo.*`: `pairs`, `pubmed_results`, `search_progress`, `pubtator_results`, `pubtator_search_progress`, `kb_events`, `kb_objects`, `alignedResults`) must be converted to Domain Models (`domain.model.*`) via mappers before returning to the application layer in all new and refactored adapters.

### 5.2. Messaging Producer Adapters (`infrastructure.adapters.out.producers.*`)
- Implement producer ports defined in `com.biopatternsg.domain.ports.out.producers.*`:
    - `NcbiQueueSenderRabbitAdapter` (`NcbiQueueSender`): Publishes `NcbiSearchRequest` messages via `@Channel("ncbi-out")` (`exchange-ncbi-pubmed`, routing key `ncbi-pubmed-queue`).
    - `PubtatorQueueSenderRabbitAdapter` (`PubtatorQueueSender`): Publishes `PubtatorSearchRequest` batch messages via `@Channel("pubtator-out")` (`exchange-pubtator`, routing key `pubtator-queue`).

### 5.3. External & Internal Service Adapters (`infrastructure.adapters.out.*` & `external_repositories.*`)
- External biological APIs and internal microservices are consumed via MicroProfile REST Clients (`infrastructure.clients.external.*`, `infrastructure.clients.internal.*`) and service wrappers (`infrastructure.external_services.*`, `infrastructure.internal_services.*`), and exposed to the domain/application layers exclusively through `*Adapter` classes implementing `domain.ports.out.external_repositories.*`:
    - **NCBI E-utilities ESearch (`ncbi-esearch-api`):** `NcbiESearchClient` -> `QueryNcbiESearch` (rate-limited with `SimpleRateLimiter`) -> `NcbiSearchAdapter` (implements `NcbiSearchRepoWeb`).
    - **PubTator3 API (`pubtator-api`):** `PubtatorClient` -> `QueryPubtator` (rate-limited with `SimpleRateLimiter`) -> `PubtatorSearchAdapter` (implements `PubtatorSearchRepoWeb`).
    - **`search-biological-objects` (`biological-objects-api`):** `BiologicalObjectsHttpClient` -> `QueryBiologicalObjects` -> `BiologicalObjectsRepositoryAdapter` (implements `BiologicalObjectsRepository`).
    - **`config-and-control` (`config-and-control-api`):** `ConfigAndControlHttpClient` -> `QueryConfigAndControl` -> `ConfigAndControlRepositoryAdapter` (implements `ConfigAndControlRepository`).
    - **`build-knowledge-base` (`build-knowledge-base-api`):** `BuildKnowledgeBaseHttpClient` -> `BuildKnowledgeBaseAdapter` (implements `BuildKnowledgeBaseRepoWeb`).

---

## 6. Architectural Naming & Verification Gates

### 6.1. Mandatory Naming Conventions (Enforced by ArchUnit)
- Classes in `application.usecase..` must end with `UseCase` (e.g., `BuildPubmedPairsUseCase`, `GenerateKbForPipelineUseCase`, `GetKbEventsByRestrictionUseCase`).
- Classes in `infrastructure.adapters.in.restcontrollers..` must end with `Controller` (e.g., `PubmedController`).
- Classes in `infrastructure.adapters.out..` must end with `Adapter` (e.g., `KbEventRepositoryAdapter`, `KbObjectRepositoryAdapter`, `NcbiSearchAdapter`, `PubtatorSearchAdapter`, `BuildKnowledgeBaseAdapter`, `NcbiQueueSenderRabbitAdapter`, `PubtatorQueueSenderRabbitAdapter`).

### 6.2. Mandatory Verification Commands
Before completing any task, verify compilation, architectural compliance, and unit tests:
```bash
# 1. Compile classes (required before ArchUnit imports target/classes)
./mvnw clean compile

# 2. Run Hexagonal Architecture and Documentation Governance validation tests
./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest

# 3. Run the full unit test suite
./mvnw test
```

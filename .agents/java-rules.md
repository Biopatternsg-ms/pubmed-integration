# Java Language and Clean Code Rules

> **Service:** `pubmed-integration`  
> **Language:** Java 21  
> **Framework:** Quarkus 3.31.4 | Lombok

This document establishes the mandatory **Java coding standards, method design, lambda practices, and static mapper conventions** that developers and AI agents must follow when writing or refactoring Java code in `pubmed-integration`.

*(For Hexagonal Architecture boundaries, layer responsibilities, and verification gates, see [`architecture-rules.md`](architecture-rules.md)).*

---

## 1. Method Signatures and Parameter Encapsulation

### 1.1. Maximum 3 Parameters Rule
- No method in a Use Case, Application Service, or Adapter should accept more than **3 individual parameters**.
- When an operation requires more than 3 inputs, encapsulate them into a Java `record`, Domain Model, or Command/Query object (e.g., `BuildPairsRequest`, `GenerateAlignedObjectsRequest`, `KbEventsByRestrictionRequest`, `NcbiSearchRequest`, `PubtatorSearchRequest`, `AlignedResult`, `KbEvent`, `KbObject`).

### 1.2. Single-Responsibility Private Methods
- Break down complex multi-step methods into small, single-responsibility private methods with names that express clear intent (e.g., `resolveKnowledgeBase(...)`, `persistKbEvents(...)`, `filterUncachedPmids(...)`, `calculateAlignedObjects(...)`, `buildInvertedSynonymsIndex(...)`).
- Public methods should read top-to-bottom as a clean sequence of high-level steps without deep nesting (keeping Sonar Cognitive Complexity low).

---

## 2. Clean Code in Streams and Lambda Expressions

### 2.1. Zero Business Logic & Zero Multi-line Builders Inside Lambdas
- Never embed multi-line business calculations, conditional `if/else` branching, try/catch blocks, or multi-line object builders inside stream/optional lambda expressions (`.map(...)`, `.filter(...)`, `.forEach(...)`, `.orElseGet(...)`).
- Lambdas must be single-expression calls or method references (`this::helperMethod`, `KbEventMapper::collectionToModel`, `KbObjectMapper::collectionToModel`) that delegate to descriptive private methods or static mappers.

```java
// ❌ INCORRECT: Multi-line object construction or logic inside lambda
List<KbEvent> events = collections.stream()
        .map(entity -> {
            List<String> pmids = entity.getPubmedIds() != null ? entity.getPubmedIds() : List.of();
            return new KbEvent(entity.getFirst(), entity.getRelation(), entity.getSecond(), pmids);
        })
        .toList();

// ✅ CORRECT: Lambda delegates via method reference to a helper or static mapper
List<KbEvent> events = collections.stream()
        .map(KbEventMapper::collectionToModel)
        .toList();
```

### 2.2. Modern Java 21 Idioms
- Prefer immutable stream terminal collection `.toList()` over `Collectors.toList()` when mutability is not required.
- Use Java `record` types for immutable Domain Models (`com.biopatternsg.domain.model.*`, e.g., `BiologicalObject`, `NcbiSearchRequest`, `PubtatorResult`, `KbEvent`, `KbObject`, `AlignedResult`, `Publication`, `PaginatedResult`) and DTOs (`com.biopatternsg.infrastructure.adapters.dtos.*`, `com.biopatternsg.infrastructure.clients.dtos.*`).
- Use pattern matching and enhanced `switch` expressions (`switch (step) { case COMBINATIONS -> ... }`) for exhaustive enum handling (e.g., `PipelineSteps`, `Status`) without fall-through bugs.

---

## 3. Dependency Injection and Class Design

### 3.1. Constructor Injection over Field Injection
- Prefer `private final` fields with constructor injection (`@RequiredArgsConstructor` from Lombok or an explicit constructor with `@Inject` / `@RestClient` / `@ConfigProperty`) over mutable `@Inject` field injection.
- Never leave unused injected fields in any class.

### 3.2. Immutability and Null Safety
- Use `Objects.equals(a, b)` for null-safe equality checks.
- Guard collection and map parameters and return empty collections (`Collections.emptyList()`, `Collections.emptyMap()`, or `List.of()`) rather than `null` for collection return types.
- Avoid dead/no-op statements (such as standalone `Response.ok().build();` calls whose return value is ignored) and ensure JAX-RS `Response` objects returned by REST clients are properly closed (e.g., with `try-with-resources`).

### 3.3. Explicit `toString()` on Domain Models and DTOs (`record` / `@ToString`)
- Every class in `domain.model.*`, `infrastructure.adapters.dtos.*`, and `infrastructure.clients.dtos.*` **must** define an explicit `toString()` implementation—either automatically as a Java `record` or via Lombok's `@ToString` / `@Data` annotation for non-record classes—instead of relying on `Object.toString()`.
- This prevents `Use of default toString()` static analysis findings (e.g., GitHub Code Quality / CodeQL) and ensures meaningful log/debug representations.
- Exclude sensitive fields (e.g., API keys, passwords, tokens, secrets) with `@ToString.Exclude` whenever applicable.

### 3.4. Log Injection Prevention (CWE-117 / CodeQL)
- **Never** log raw user-provided, RabbitMQ message, or external API values directly via SLF4J (`log.info`, `log.warn`, `log.error`, `log.debug`). Unsanitized line breaks (`\n`, `\r`) allow log forging and trigger GitHub CodeQL `java/log-injection` alerts.
- Always wrap untrusted/external values with `com.biopatternsg.domain.util.LogSanitizer.sanitize(value)`, which safely converts `null` and replaces `\n` and `\r` with `_`.

```java
// ❌ INCORRECT: Raw user-provided value logged directly (CodeQL java/log-injection)
log.info("Executing GetKbEventsByTermUseCase for pipelineId=[{}], term=[{}]", pipelineId, term);

// ✅ CORRECT: Untrusted values sanitized before logging
log.info("Executing GetKbEventsByTermUseCase for pipelineId=[{}], term=[{}]",
        LogSanitizer.sanitize(pipelineId), LogSanitizer.sanitize(term));
```

---

## 4. Static Mappers (`com.biopatternsg.infrastructure.mapper`)

### 4.1. Stateless Utility Classes and Pure Functions
For all new or refactored mapper classes under `com.biopatternsg.infrastructure.mapper.*` (e.g., refactoring `PubtatorResultMapper` or adding `KbEventMapper`, `KbObjectMapper`, `AlignedResultMapper`):
- **Class Definition:** Every mapper class must be declared `public final class` with a `private` constructor to prevent instantiation.
- **Pure Functions:**
    - All mapping methods must be `public static`, deterministic, and free of side effects.
    - Zero mutable state or instance fields.
    - No CDI annotations (`@ApplicationScoped`, `@Singleton`, `@RequestScoped`).
    - No runtime reflection or bytecode-generation libraries.
- **Graceful Null Safety:** Every mapping method must safely handle `null` inputs by returning `null` or an empty collection (`List.of()`) without throwing `NullPointerException`.

### 4.2. Standardized Method Naming Conventions
All static mappers should follow clear, consistent naming conventions:

1. **Request DTO → Domain Model:**
    - `requestToModel(XRequest request)`
2. **Database Collection / Entity → Domain Model:**
    - `collectionToModel(KbEventCollection collection)` / `collectionToModel(KbObjectCollection collection)`
    - `collectionToModelList(List<KbEventCollection> list)` / `collectionToModelList(List<KbObjectCollection> list)`
3. **Domain Model → Database Collection / Entity:**
    - `modelToCollection(AlignedResult model)` / `modelToCollection(PubtatorResult model)`
    - `modelToCollectionList(List<PubtatorResult> list)`
4. **Domain Model → Delivery / Response DTO:**
    - `fromDomain(XModel model)` / `modelToResponse(XModel model)`
    - `modelToResponseList(List<XModel> list)`
5. **External Client / Provider DTO → Domain Model:**
    - `<provider>ToModel(ProviderResponse response)` (e.g., `pubtatorToModel(PubtatorDocumentDto doc)`, `ncbiToModel(NcbiESearchResponse response)`, `buildKbToModel(GenerateKbResponse response)`)
    - `<provider>ToModelList(List<ProviderResponse> list)`

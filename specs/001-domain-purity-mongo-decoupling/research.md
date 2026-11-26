# Phase 0 Research: Domain and Application Layer Purity and MongoDB Decoupling

**Feature**: `001-domain-purity-mongo-decoupling` | **Date**: 2026-11-24

## 1. Relocating MongoDB Panache Entities

- **Decision**: Move all 9 classes from `com.biopatternsg.mongo` to `com.biopatternsg.infrastructure.mongo` without altering `@MongoEntity(collection = ...)` annotations or field names/types.
- **Rationale**: `ArchitectureLayerTest` enforces isolation of `com.biopatternsg.domain..` and `com.biopatternsg.application..` against `com.biopatternsg.infrastructure..`. Moving the Panache entities into `com.biopatternsg.infrastructure.mongo` brings all persistence entities under the governed infrastructure boundary while preserving 100% BSON schema compatibility with existing MongoDB collections (`pairs`, `pubmed_results`, `search_progress`, `pubtator_results`, `pubtator_search_progress`, `kb_events`, `kb_objects`, `alignedResults`).
- **Alternatives Considered**:
  - Adding an ArchUnit rule banning `com.biopatternsg.mongo..` without moving the classes: Rejected because it leaves persistence entities outside `infrastructure`, violating Principle I of the Constitution.

## 2. Domain Models for Pairs and Search Progress

- **Decision**:
  - Introduce `TermPair(String firstTerm, String secondTerm)` and `PipelinePairsBatch(String pipelineId, List<TermPair> pairs)` as Java `record`s in `com.biopatternsg.domain.model`.
  - Introduce `SearchProgress(String pipelineId, int completedCount, int totalCount, String userId)` as a Java `record` in `com.biopatternsg.domain.model` with `public boolean isCompleted()`.
- **Rationale**:
  - `PairsCollection` represents a batch of pairs for a `pipelineId` (multiple documents can exist per `pipelineId` across levels). `PipelinePairsBatch` preserves this exact batching contract in pure domain terms without exposing MongoDB `ObjectId` or Panache entity inheritance.
  - Both `SearchProgressCollection` and `PubtatorSearchProgressCollection` share the exact same fields (`pipelineId`, `completedCount`, `totalCount`, `userId`) and semantics, so a single `SearchProgress` domain record cleanly serves both `SearchProgressRepository` and `PubtatorSearchProgressRepository`.
  - Note: `com.biopatternsg.domain.model.Pair<F, S>` already exists as a generic tuple used internally for object-ID pairing in `BuildPubmedPairsUseCase`; using `TermPair` and `PipelinePairsBatch` avoids naming collisions and makes the domain model explicit.
- **Alternatives Considered**:
  - Flattening `PairRepository` to `save(String pipelineId, List<TermPair> pairs)`: Rejected because `PipelinePairsBatch` encapsulates the 2-field batch cohesive unit and aligns with the Constitution's 3-parameter limit and existing `findByPipelineId` return structure.

## 3. Stateless Static Mappers (Constitution Principle V)

- **Decision**: Create `public final` utility mapper classes in `com.biopatternsg.infrastructure.mapper` with private constructors, zero CDI annotations, and null-safe `public static` methods:
  - `PairMapper` (`collectionToModel`, `modelToCollection`)
  - `SearchProgressMapper` (`collectionToModel`)
  - `PubtatorSearchProgressMapper` (`collectionToModel`)
  - `ExpertObjectConfigMapper` (`requestToModel`)
- **Rationale**: Constitution Principle V explicitly mandates stateless `public final` static mappers with standardized method names (`collectionToModel`, `modelToCollection`, `requestToModel`) and zero CDI annotations for all new or refactored mappers.
- **Alternatives Considered**:
  - `@ApplicationScoped` CDI mappers (like legacy `PubtatorResultMapper`): Rejected for new/refactored mappers as it violates Constitution Principle V.

## 4. Decoupling `ExpertObjectConfig` from Jackson

- **Decision**: Remove `@JsonIgnoreProperties(ignoreUnknown = true)` and the Jackson import from `com.biopatternsg.domain.model.ExpertObjectConfig`. Introduce `ExpertObjectConfigDto` (with `@JsonIgnoreProperties(ignoreUnknown = true)`) in `com.biopatternsg.infrastructure.adapters.dtos`, update `GenerateAlignedObjectsRequest` to use `List<ExpertObjectConfigDto>`, and map to `List<ExpertObjectConfig>` in `PubmedController` via `ExpertObjectConfigMapper.requestToModel(...)`.
- **Rationale**: Preserves exact JSON deserialization behavior for `POST /pubmed/generate-aligned-objects` (including ignoring unknown properties) while keeping `domain.model.ExpertObjectConfig` 100% framework-free.
- **Alternatives Considered**:
  - Configuring Jackson globally to ignore unknown properties: Rejected because it would silently change deserialization behavior for all other DTOs across the service.

## 5. Decoupling `GenerateAlignedObjectsUseCase` from `quarkus-exceptions-handler`

- **Decision**: Introduce `com.biopatternsg.domain.exception.AlignmentException` extending `RuntimeException` and throw it from `GenerateAlignedObjectsUseCase` instead of `com.cifertech.exceptionhandler.exceptions._5xx.InternalServerError`.
- **Rationale**: Keeps `application` free of HTTP/web exception dependencies. `PubmedController.generateAlignedObjects` already executes the use case asynchronously inside `CompletableFuture.runAsync(...)` with a `try/catch` that logs errors, so external `202 Accepted` HTTP behavior is unchanged. For any future synchronous caller, a JAX-RS `ExceptionMapper<AlignmentException>` in `infrastructure` can map it to HTTP 500 `InternalServerError` if needed, or the domain exception can carry the cause cleanly.
- **Alternatives Considered**:
  - Swallowing the exception in `GenerateAlignedObjectsUseCase`: Rejected because callers (and unit tests) expect failure propagation after `Status.FAILED` is reported.

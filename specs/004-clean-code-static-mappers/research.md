# Research & Technical Decisions: Clean Code, DI, Null Safety, `toString()`, and Static Mappers

**Feature**: `004-clean-code-static-mappers`
**Date**: 2026-11-25

## 1. Stateless Pure Static Mappers

- **Decision**: Refactor `PubtatorResultMapper` into `public final class PubtatorResultMapper` with a `private PubtatorResultMapper()` constructor, zero CDI annotations, and `public static` null-safe methods (`pubtatorToModel`, `collectionToModel`, `modelToCollection`, `collectionToModelList`, `modelToCollectionList`). Create 4 new `public final` static mappers (`AlignedResultMapper`, `KbEventMapper`, `KbObjectMapper`, `BuildKnowledgeBaseMapper`) following the exact same pattern already established by `PairMapper`, `SearchProgressMapper`, `PubtatorSearchProgressMapper`, and `ExpertObjectConfigMapper`.
- **Rationale**: Eliminates CDI proxy overhead and instance state, removes ~150 lines of inline mapping from `PubtatorResultRepositoryAdapter` and 4 duplicated `new KbObject(...)` blocks from `KbObjectRepositoryAdapter`, and makes all mappers directly unit-testable without mocks.
- **Alternatives considered**: MapStruct or ModelMapper — explicitly out of scope per Requirement 04.

## 2. Converting Client DTOs to Java Records

- **Decision**: Convert `NcbiESearchResponse` and `NcbiESearchResultDto` to Java `record` types with `@JsonIgnoreProperties(ignoreUnknown = true)` and `@JsonProperty(...)` annotations on record components, updating callers in `QueryNcbiESearchImpl` (`eSearchResult()`, `idList()`).
- **Rationale**: Java `record`s provide immutability and a deterministic, human-readable `toString()`, `equals()`, and `hashCode()` automatically without Lombok boilerplate, and Jackson natively supports `@JsonProperty` on record components.
- **Alternatives considered**: Adding `@ToString` to mutable `@Getter @Setter` classes — works, but `record`s are preferred per Constitution Principle IV.

## 3. Constructor Injection & Unused Annotation Cleanup

- **Decision**:
  - `NcbiQueueSenderRabbitAdapter` and `PubtatorQueueSenderRabbitAdapter`: inject `@Channel(...) Emitter<...>` via explicit constructor into `private final` fields.
  - `SearchPubtatorByPmidsUseCase`: inject `@ConfigProperty(name = "pubtator.batch-size", defaultValue = "50") int batchSize` via explicit constructor into a `private final` field (matching `BuildPubmedPairsUseCase`).
  - Note: `QueryConfigAndControlImpl` was already cleaned up in feature 03; verify no redundant `@RestClient @Inject` field annotations remain.
  - Remove unused `@Slf4j` (`BiologicalObjectsServiceImpl`, `PubmedResultRepositoryAdapter`, `PubtatorResultRepositoryAdapter`), unused `@RequiredArgsConstructor` (`PairRepositoryAdapter`), and unused `import java.util.Map;` (`KbObjectCollection`).
- **Rationale**: Explicit constructor injection guarantees immutability and allows direct instantiation in unit tests without Quarkus/Mockito field injection.

## 4. Null-Safety & Pagination Bounds Guards

- **Decision**:
  - In `AlignedResultMapper.modelToCollection(AlignedResult)`: guard `alignedResult.alignedAs() != null` (defaulting to `List.of()`).
  - In `KbObjectRepositoryAdapter.findByPipelineId(String pipelineId, int page, int size)`: clamp `int safePage = Math.max(page, 0);`; if `size <= 0`, return `new PaginatedResult<>(List.of(), safePage, 0, totalItems, 0)` without dividing by zero.
- **Rationale**: Prevents `NullPointerException` on partial `AlignedResult` objects and `ArithmeticException` / invalid offsets on non-positive `size` or negative `page`.

# Research & Technical Decisions: Outbound Adapters Business-Logic Extraction and Parameter Encapsulation

**Feature**: `003-outbound-adapters-param-encapsulation`
**Date**: 2026-11-25

## 1. Restriction Level Filtering Placement

- **Decision**: Introduce `RestrictionLevel` enum (`UNRESTRICTED`, `RESTRICTED`, `VERY_RESTRICTED`) in `com.biopatternsg.domain.model` with a `fromString(String)` factory (`null`, blank, and `"NO_RESTRICTED"` map to `UNRESTRICTED`). Move the filtering logic into `GetKbEventsByRestrictionUseCase` using `kbEventRepository.findByPipelineId(pipelineId)` and a Java 21 enhanced `switch` expression, and remove `findByPipelineIdAndRestriction(...)` from `KbEventRepository` and `KbEventRepositoryAdapter`.
- **Rationale**: `KbEventRepositoryAdapter.findByPipelineIdAndRestriction` already loaded `findByPipelineId(pipelineId)` into memory and applied business rules in Java. Moving the filtering into `GetKbEventsByRestrictionUseCase` preserves identical runtime behavior while eliminating business branching from the persistence adapter and making restriction rules unit-testable without MongoDB.
- **Alternatives considered**:
  - Keeping `findByPipelineIdAndRestriction` in `KbEventRepositoryAdapter` with MongoDB aggregation: explicitly out of scope for this refactoring.
  - Creating a separate `KbEventFilterService`: unnecessary extra indirection since `GetKbEventsByRestrictionUseCase` has no other responsibilities and remains a concise, single-purpose orchestrator.

## 2. Centralizing `@PREFIX_SUFFIX` Biological Term Normalization

- **Decision**: Create `com.biopatternsg.domain.util.BiologicalTermNormalizer` as a `public final` stateless utility class with a private constructor and `public static` methods:
  - `NormalizedTerm normalize(String rawTerm)` returning a `record NormalizedTerm(String biotypePrefix, String term)` (where `biotypePrefix` is uppercase or `null` if no `@PREFIX_SUFFIX` pattern matched, and `term` is the trimmed suffix or trimmed original term).
  - `String extractSuffix(String rawTerm)` convenience method returning the normalized suffix (or `rawTerm` as-is if `null`/blank).
- **Rationale**: Both `KbEventRepositoryAdapter` and `KbObjectRepositoryAdapter` duplicated `Pattern.compile("^@([^_]+)_(.+)$")` with slightly different handling (events only needed the suffix; objects needed both the uppercase biotype prefix and suffix). A single pure domain utility eliminates duplication and enables thorough unit testing.
- **Alternatives considered**:
  - Placing normalization in an `@ApplicationScoped` service: rejected because persistence adapters cannot depend on `application.*` per Hexagonal Layer Isolation (`ArchitectureLayerTest`), whereas `domain.util.*` is accessible to all layers.

## 3. Thread-Safe Parallel Pair Generation & Application Service Extraction

- **Decision**: Extract pair generation into `PubmedPairsBuilderService` (`application.services`) + `PubmedPairsBuilderServiceImpl` (`application.services.impl`, `@ApplicationScoped`), and alignment into `ExpertObjectAlignmentService` (`application.services`) + `ExpertObjectAlignmentServiceImpl` (`application.services.impl`, `@ApplicationScoped`).
  In `PubmedPairsBuilderServiceImpl.processHigherLevel`, replace `Set<PairsGenerator.TermsPair> uniquePairs = new HashSet<>(); biologicalObjectFamily.parallelStream().forEach(...)` with:
  `biologicalObjectFamily.parallelStream().filter(obj -> !obj.id().equals(currentBiologicalObject.id())).flatMap(obj -> buildTermsBetweenObjects(useOnlyPrincipalName, currentBiologicalObject, obj).stream()).collect(Collectors.toSet())`.
- **Rationale**: Mutating an unsynchronized `HashSet` inside `parallelStream().forEach(...)` causes race conditions, lost elements, and `ConcurrentModificationException`. Functional `.flatMap(...).collect(Collectors.toSet())` is thread-safe by design (Collector creates per-thread containers and merges them) and requires no shared mutable state.
- **Alternatives considered**:
  - `ConcurrentHashMap.newKeySet()`: works, but functional `flatMap().collect(Collectors.toSet())` is cleaner, idiomatic Java 21, and avoids side effects in stream lambdas.

## 4. Parameter Encapsulation Records

- **Decision**: Introduce immutable Java `record`s in `com.biopatternsg.domain.model` for every >3-parameter signature:
  - `BuildPubmedPairsCommand(String pipelineId, boolean useShortName, int levels, String userId)` for `BuildPubmedPairs.execute(BuildPubmedPairsCommand)`.
  - `PipelineStepUpdate(String pipelineId, PipelineSteps step, Status status, String userId, Map<String, String> metrics)` with static factory methods `of(pipelineId, step, status, userId)` and `withMetrics(pipelineId, step, status, userId, metrics)` for `ConfigAndControlRepository.updateStep(PipelineStepUpdate)` and `QueryConfigAndControl.updateStep(PipelineStepUpdate)`.
  - `KbEventKey(String pipelineId, String first, String relation, String second)` for `KbEventRepository.findByRelation(KbEventKey)`.
  - Reuse existing `KbEvent(String pipelineId, String first, String relation, String second, List<String> pubmedIds)` for `KbEventRepository.upsert(KbEvent)`.
  - Reuse existing `PubtatorResult` for `BuildKnowledgeBaseRepoWeb.generateKnowledgeBase(String pipelineId, PubtatorResult pubtatorResult)` (reading `pmid`, `title`, `text`, `objects`, `events` inside `BuildKnowledgeBaseAdapter`).
  - `KbObjectUpsertCommand(String rawName, List<String> synonyms, Map<String, String> biotypes, List<String> itemBiotypes)` for `KbObjectRepositoryAdapter.upsertKbObject(String pipelineId, KbObjectUpsertCommand)`.
- **Rationale**: Java `record`s are immutable, concise, provide value-based `equals`/`hashCode`/`toString()` automatically (satisfying Constitution Principle IV), and keep domain models framework-free.
- **Alternatives considered**:
  - Lombok `@Value` / `@Builder` classes: more boilerplate than Java 21 `record`s and less idiomatic for pure immutable value objects.

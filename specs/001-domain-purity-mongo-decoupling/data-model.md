# Data Model: Domain and Application Layer Purity and MongoDB Decoupling

**Feature**: `001-domain-purity-mongo-decoupling` | **Date**: 2026-11-24

## 1. Pure Domain Models (`com.biopatternsg.domain.model`)

### `TermPair` (New `record`)
- **Fields**:
  - `String firstTerm`
  - `String secondTerm`
- **Purpose**: Represents an ordered pair of biological/search terms to be queried in PubMed.
- **Constraints**: Immutable Java `record`; zero framework annotations.

### `PipelinePairsBatch` (New `record`)
- **Fields**:
  - `String pipelineId`
  - `List<TermPair> pairs`
- **Purpose**: Represents a persisted batch of `TermPair` entries associated with a pipeline execution.
- **Validation / Null-Safety**: Compact constructor or mapper ensures `pairs` is never `null` (defaults to `List.of()` when null).

### `SearchProgress` (New `record`)
- **Fields**:
  - `String pipelineId`
  - `int completedCount`
  - `int totalCount`
  - `String userId`
- **Methods**:
  - `public boolean isCompleted()` — returns `completedCount == totalCount`.
- **Purpose**: Tracks atomic completion progress for both NCBI pair searches and PubTator batch searches.

### `ExpertObjectConfig` (Updated `record`)
- **Fields**:
  - `String uniprotId`
  - `String hgncId`
  - `String symbol`
- **Change**: Removed `@JsonIgnoreProperties(ignoreUnknown = true)` and `com.fasterxml.jackson.*` import; remains a pure Java `record`.

---

## 2. Domain Exceptions (`com.biopatternsg.domain.exception`)

### `AlignmentException` (New `RuntimeException`)
- **Constructors**:
  - `AlignmentException(String message, Throwable cause)`
  - `AlignmentException(Throwable cause)`
- **Purpose**: Signals an unrecoverable failure during aligned-object generation without coupling `application` to HTTP exception classes.

---

## 3. Infrastructure DTOs (`com.biopatternsg.infrastructure.adapters.dtos`)

### `ExpertObjectConfigDto` (New `record`)
- **Annotations**: `@JsonIgnoreProperties(ignoreUnknown = true)`
- **Fields**:
  - `String uniprotId`
  - `String hgncId`
  - `String symbol`
- **Usage**: Embedded in `GenerateAlignedObjectsRequest` as `List<ExpertObjectConfigDto> expertObjects`.

---

## 4. Infrastructure Persistence Entities (`com.biopatternsg.infrastructure.mongo`)

All 9 existing Panache MongoDB entities are relocated from `com.biopatternsg.mongo.*` to `com.biopatternsg.infrastructure.mongo.*` with **zero changes** to `@MongoEntity(collection = ...)` or field definitions:
1. `AlignedAsEmbedded`
2. `AlignedResultCollection` (`@MongoEntity(collection = "alignedResults")`)
3. `KbEventCollection` (`@MongoEntity(collection = "kb_events")`)
4. `KbObjectCollection` (`@MongoEntity(collection = "kb_objects")`)
5. `PairsCollection` (`@MongoEntity(collection = "pairs")`)
6. `PubmedResultCollection` (`@MongoEntity(collection = "pubmed_results")`)
7. `PubtatorResultCollection` (`@MongoEntity(collection = "pubtator_results")`)
8. `PubtatorSearchProgressCollection` (`@MongoEntity(collection = "pubtator_search_progress")`)
9. `SearchProgressCollection` (`@MongoEntity(collection = "search_progress")`)

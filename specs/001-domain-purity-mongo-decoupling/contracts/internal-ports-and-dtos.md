# Internal Ports & Boundary Contracts

**Feature**: `001-domain-purity-mongo-decoupling` | **Date**: 2026-11-24

## 1. Outbound Repository Ports (`com.biopatternsg.domain.ports.out.repositories`)

### `PairRepository`
- `void save(PipelinePairsBatch batch)`
- `void deleteByPipelineId(String pipelineId)`
- `List<PipelinePairsBatch> findByPipelineId(String pipelineId)`

### `SearchProgressRepository`
- `void initializeProgress(String pipelineId, int totalCount, String userId)`
- `SearchProgress incrementAndGet(String pipelineId)`
- `void deleteByPipelineId(String pipelineId)`

### `PubtatorSearchProgressRepository`
- `void initializeProgress(String pipelineId, int totalCount, String userId)`
- `SearchProgress incrementAndGet(String pipelineId)`
- `void deleteByPipelineId(String pipelineId)`

---

## 2. Stateless Infrastructure Mappers (`com.biopatternsg.infrastructure.mapper`)

All mappers are `public final` utility classes with private constructors and `public static` null-safe methods:

- **`PairMapper`**:
  - `public static PipelinePairsBatch collectionToModel(PairsCollection collection)`
  - `public static List<PipelinePairsBatch> collectionListToModel(List<PairsCollection> collections)`
  - `public static PairsCollection modelToCollection(PipelinePairsBatch batch)`
- **`SearchProgressMapper`**:
  - `public static SearchProgress collectionToModel(SearchProgressCollection collection)`
- **`PubtatorSearchProgressMapper`**:
  - `public static SearchProgress collectionToModel(PubtatorSearchProgressCollection collection)`
- **`ExpertObjectConfigMapper`**:
  - `public static ExpertObjectConfig requestToModel(ExpertObjectConfigDto dto)`
  - `public static List<ExpertObjectConfig> requestListToModel(List<ExpertObjectConfigDto> dtos)`

---

## 3. External REST Contract (Unchanged)

### `POST /pubmed/generate-aligned-objects`
- **Request Body (`GenerateAlignedObjectsRequest`)**:
  ```json
  {
    "pipelineId": "string (required, non-blank)",
    "expertObjects": [
      {
        "uniprotId": "string",
        "hgncId": "string",
        "symbol": "string"
      }
    ]
  }
  ```
- **Backward Compatibility**: Unknown JSON fields inside `expertObjects[]` items are ignored via `@JsonIgnoreProperties(ignoreUnknown = true)` on `ExpertObjectConfigDto`.
- **Response**: `202 Accepted` with `{"message": "Aligned objects generation started"}`.

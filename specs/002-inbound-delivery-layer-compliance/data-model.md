# Data Model: Inbound Consumers, REST Controller and Lifecycle Delivery-Layer Compliance

**Feature**: `002-inbound-delivery-layer-compliance` | **Date**: 2026-11-24

## Existing Domain Models Used (Unchanged)

- **`NcbiSearchRequest`** (`com.biopatternsg.domain.model.NcbiSearchRequest`):
  - Fields: `pipelineId`, `term`, `retmax`, `pairIndex`, `pairsTotal`, `userId`
  - Passed from `NcbiRabbitConsumer` to `ProcessNcbiSearchRequest.execute(NcbiSearchRequest)`.

- **`PubtatorSearchRequest`** (`com.biopatternsg.domain.model.PubtatorSearchRequest`):
  - Fields: `pipelineId`, `pmids`, `batchIndex`, `batchesTotal`, `userId`
  - Passed from `PubtatorRabbitConsumer` to `ProcessPubtatorBatch.execute(PubtatorSearchRequest)`.

- **`PubtatorResult`** (`com.biopatternsg.domain.model.PubtatorResult`):
  - Returned as `List<PubtatorResult>` from `PubtatorSearchRepoWeb.search(List<String> pmids)` after parsing inside `PubtatorSearchAdapter`.

- **`SearchProgress`** (`com.biopatternsg.domain.model.SearchProgress`):
  - Used by `ProcessNcbiSearchRequestUseCase` and `ProcessPubtatorBatchUseCase` via `SearchProgressRepository` / `PubtatorSearchProgressRepository` to detect `progress.isCompleted()`.

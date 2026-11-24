# Use Cases Catalog — `pubmed-integration`

This directory contains the functional and technical documentation for every Use Case implemented in the `pubmed-integration` microservice (`com.biopatternsg.application.usecase.*`).

> **Language Policy:** All documentation under `.agents/use_cases/` MUST be written in English.

## Index

| Use Case | Inbound Port | Entry Channel | Primary Responsibility | Documentation |
|---|---|---|---|---|
| `BuildPubmedPairsUseCase` | `BuildPubmedPairs` | REST (`POST /pubmed/build-pairs`, async) | Queries biological objects across pipeline levels from `search-biological-objects`, builds unique term/synonym pairs, persists them in `pairs`, and reports `COMBINATIONS` completion. | [`BuildPubmedPairsUseCase.md`](BuildPubmedPairsUseCase.md) |
| `SearchPubmedByPairsUseCase` | `SearchPubmedByPairs` | REST (`POST /pubmed/search-pubmed-ids-by-pairs`, async) | Loads generated pairs for a pipeline, initializes `search_progress`, and enqueues `firstTerm AND secondTerm` queries to RabbitMQ (`ncbi-out`). | [`SearchPubmedByPairsUseCase.md`](SearchPubmedByPairsUseCase.md) |
| `SearchPubtatorByPmidsUseCase` | `SearchPubtatorByPmids` | REST (`POST /pubmed/search-pubtator-by-pmids`, async) | Reads numeric PMIDs for a pipeline, partitions them into configured batches, initializes `pubtator_search_progress`, and enqueues batches to RabbitMQ (`pubtator-out`). | [`SearchPubtatorByPmidsUseCase.md`](SearchPubtatorByPmidsUseCase.md) |
| `GenerateKbForPipelineUseCase` | `GenerateKbForPipeline` | REST (`POST /pubmed/generate-kb`, async) | Processes pipeline PMIDs in parallel, resolves Knowledge Base data (from `pubtator_results` cache or `build-knowledge-base`), persists `kb_events` and `kb_objects`, cleans `pubmed_results`, and reports `BUILD_KNOWLEDGE_BASE` completion. | [`GenerateKbForPipelineUseCase.md`](GenerateKbForPipelineUseCase.md) |
| `GenerateAlignedObjectsUseCase` | `GenerateAlignedObjects` | REST (`POST /pubmed/generate-aligned-objects`, async) | Aligns expert object symbols against the pipeline synonym dictionary in `kb_objects`, persists the alignment breakdown in `alignedResults`, and reports `GENERATE_ALIGNED_OBJECTS` completion. | [`GenerateAlignedObjectsUseCase.md`](GenerateAlignedObjectsUseCase.md) |
| `GetPaginatedKbObjectsUseCase` | `GetPaginatedKbObjects` | REST (`GET /pubmed/kb-objects/{pipelineId}`, `GET /pubmed/synonyms/{pipelineId}`) | Returns a paginated list of KB objects (`kb_objects`) for a pipeline. | [`GetPaginatedKbObjectsUseCase.md`](GetPaginatedKbObjectsUseCase.md) |
| `GetKbObjectByNameUseCase` | `GetKbObjectByName` | REST (`GET /pubmed/kb-objects/{pipelineId}/by-name/{name}`, `GET /pubmed/synonyms/{pipelineId}/by-name/{name}`) | Retrieves a single KB object (`KbObject`) by pipeline ID and name. | [`GetKbObjectByNameUseCase.md`](GetKbObjectByNameUseCase.md) |
| `GetAllKbObjectsByPipelineUseCase` | `GetAllKbObjectsByPipeline` | REST (`GET /pubmed/kb-objects/{pipelineId}/all`) | Retrieves all KB objects belonging to a pipeline without pagination. | [`GetAllKbObjectsByPipelineUseCase.md`](GetAllKbObjectsByPipelineUseCase.md) |
| `UpdateKbObjectRolesUseCase` | `UpdateKbObjectRoles` | REST (`PATCH /pubmed/kb-objects/{pipelineId}/roles`) | Bulk-updates biological roles assigned to KB objects for a pipeline. | [`UpdateKbObjectRolesUseCase.md`](UpdateKbObjectRolesUseCase.md) |
| `ResetKbObjectRolesUseCase` | `ResetKbObjectRoles` | REST (`DELETE /pubmed/kb-objects/{pipelineId}/roles`) | Resets all assigned roles in `kb_objects` for a pipeline. | [`ResetKbObjectRolesUseCase.md`](ResetKbObjectRolesUseCase.md) |
| `GetKbEventsByTermUseCase` | `GetKbEventsByTerm` | REST (`GET /pubmed/kb-events/{pipelineId}/by-term/{term}`) | Retrieves KB events in a pipeline where the given term participates. | [`GetKbEventsByTermUseCase.md`](GetKbEventsByTermUseCase.md) |
| `GetKbEventsByPipelineUseCase` | `GetKbEventsByPipeline` | REST (`GET /pubmed/kb-events/{pipelineId}`) | Retrieves all KB events stored for a pipeline. | [`GetKbEventsByPipelineUseCase.md`](GetKbEventsByPipelineUseCase.md) |
| `GetKbEventsByRestrictionUseCase` | `GetKbEventsByRestriction` | REST (`POST /pubmed/kb-events/{pipelineId}/by-restriction`) | Filters pipeline KB events according to a restriction level and a list of aligned objects. | [`GetKbEventsByRestrictionUseCase.md`](GetKbEventsByRestrictionUseCase.md) |
| `GetAlignedResultsUseCase` | `GetAlignedResults` | REST (`GET /pubmed/aligned-results/{pipelineId}`) | Retrieves the persisted alignment summary (`AlignedResultSummary`) for a pipeline. | [`GetAlignedResultsUseCase.md`](GetAlignedResultsUseCase.md) |
| `GetPublicationsByPmidsUseCase` | `GetPublicationsByPmids` | REST (`POST /pubmed/publications/by-pmids`) | Deduplicates input PMIDs and retrieves publication metadata (`pmid`, `title`, `text`) from `pubtator_results`. | [`GetPublicationsByPmidsUseCase.md`](GetPublicationsByPmidsUseCase.md) |

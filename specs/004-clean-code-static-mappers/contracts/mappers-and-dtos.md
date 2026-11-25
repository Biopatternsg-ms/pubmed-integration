# Contracts: Stateless Static Mappers

**Feature**: `004-clean-code-static-mappers`
**Date**: 2026-11-25

All classes in `com.biopatternsg.infrastructure.mapper` MUST be `public final`, declare a `private` no-arg constructor, carry zero CDI annotations, and expose only `public static` null-safe methods:

- `PubtatorResultMapper`:
  - `pubtatorToModel(PubtatorApiDto)` → `PubtatorResult`
  - `collectionToModel(PubtatorResultCollection)` → `PubtatorResult`
  - `modelToCollection(PubtatorResult)` → `PubtatorResultCollection`
  - `collectionToModelList(List<PubtatorResultCollection>)` → `List<PubtatorResult>`
  - `modelToCollectionList(List<PubtatorResult>)` → `List<PubtatorResultCollection>`
  - `knowledgeBaseToCollection(PubtatorResult.KnowledgeBase)` → `PubtatorResultCollection.KnowledgeBaseDoc` (for `updateKnowledgeBase`)
- `AlignedResultMapper`:
  - `modelToCollection(AlignedResult)` → `AlignedResultCollection` (null-safe `alignedAs`)
  - `collectionToSummary(AlignedResultCollection)` → `AlignedResultSummary` (null-safe)
- `KbEventMapper`:
  - `collectionToModel(KbEventCollection)` → `KbEvent`
  - `collectionToModelList(List<KbEventCollection>)` → `List<KbEvent>`
- `KbObjectMapper`:
  - `collectionToModel(KbObjectCollection)` → `KbObject`
  - `collectionToModelList(List<KbObjectCollection>)` → `List<KbObject>`
- `BuildKnowledgeBaseMapper`:
  - `modelToDocumentRequest(String pipelineId, PubtatorResult pubtatorResult)` → `PubTatorDocumentRequest`
  - `buildKbToModel(String pipelineId, GenerateKbResponse response)` → `GenerateKbResult`

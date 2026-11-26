# Contracts: Ports & Application Services

**Feature**: `003-outbound-adapters-param-encapsulation`
**Date**: 2026-11-25

## Updated Inbound Ports (`com.biopatternsg.domain.ports.in`)

- `BuildPubmedPairs`:
  - `void execute(BuildPubmedPairsCommand command)` (1 parameter; previously 4)

## Updated Outbound Ports (`com.biopatternsg.domain.ports.out`)

- `BuildKnowledgeBaseRepoWeb`:
  - `GenerateKbResult generateKnowledgeBase(String pipelineId, PubtatorResult pubtatorResult)` (2 parameters; previously 6)
- `ConfigAndControlRepository`:
  - `void updateStep(PipelineStepUpdate update)` (1 parameter; previously 4 & 5 overloads)
- `KbEventRepository`:
  - `Optional<KbEvent> findByRelation(KbEventKey key)` (1 parameter; previously 4)
  - `void upsert(KbEvent event)` (1 parameter; previously 5)
  - `List<KbEvent> findByPipelineIdAndTerm(String pipelineId, String term)` (unchanged)
  - `List<KbEvent> findByPipelineId(String pipelineId)` (unchanged)
  - *(Removed)* `findByPipelineIdAndRestriction(String, String, List<String>)`

## Updated Internal Services (`com.biopatternsg.infrastructure.internal_services`)

- `QueryConfigAndControl`:
  - `void updateStep(PipelineStepUpdate update)` (1 parameter; previously 4 & 5 overloads)

## New Application Services (`com.biopatternsg.application.services`)

- `PubmedPairsBuilderService`:
  - `Set<TermPair> buildFirstLevelPairs(String pipelineId, boolean useOnlyPrincipalName)`
  - `void buildAndSaveHigherLevelPairs(String pipelineId, boolean useOnlyPrincipalName, int level)` *(or returns `Set<TermPair>` per level — orchestrator or service persists via `PairRepository`)*
- `ExpertObjectAlignmentService`:
  - `AlignedResult align(String pipelineId, List<ExpertObjectConfig> expertObjects, Map<String, List<String>> synonymsMap)`

## External HTTP Contracts (Unchanged)

- `PubmedController` endpoints and DTOs remain unchanged; `PubmedController` constructs `new BuildPubmedPairsCommand(pipelineId, useShortName, levels, userId)` internally.

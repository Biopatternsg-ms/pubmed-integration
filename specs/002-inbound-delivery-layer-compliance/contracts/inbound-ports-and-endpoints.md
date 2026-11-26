# Inbound Ports, Outbound Ports & Delivery Contracts

**Feature**: `002-inbound-delivery-layer-compliance` | **Date**: 2026-11-24

## 1. New Inbound Ports (`com.biopatternsg.domain.ports.in`)

- **`ProcessNcbiSearchRequest`**:
  - `void execute(NcbiSearchRequest request)`
- **`ProcessPubtatorBatch`**:
  - `void execute(PubtatorSearchRequest request)`

## 2. Updated Outbound Port (`com.biopatternsg.domain.ports.out.external_repositories`)

- **`PubtatorSearchRepoWeb`**:
  - Before: `String search(List<String> pmids)`
  - After: `List<PubtatorResult> search(List<String> pmids)`

## 3. Inbound RabbitMQ Consumers (`com.biopatternsg.infrastructure.adapters.in.consumers`)

- **`NcbiRabbitConsumer`** (`@Incoming("ncbi-in")`, `@Blocking(ordered = false)`):
  - Injects ONLY `ProcessNcbiSearchRequest`.
  - Deserializes `JsonObject` → `NcbiSearchRequest` and calls `processNcbiSearchRequest.execute(request)`.
- **`PubtatorRabbitConsumer`** (`@Incoming("pubtator-in")`, `@Blocking(ordered = false)`):
  - Injects ONLY `ProcessPubtatorBatch`.
  - Deserializes `JsonObject` → `PubtatorSearchRequest` and calls `processPubtatorBatch.execute(request)`.

## 4. REST Controller Endpoints (`PubmedController`, `/pubmed/*`)

- All route paths, HTTP methods, and response payload schemas remain unchanged.
- `buildTreeMesh` renamed to `buildPubmedPairs` on `POST /pubmed/build-pairs`.
- Declarative `@NotBlank` / `@NotNull` / `@Valid` constraints enforced on path/body parameters (`name`, `term`, `pipelineId`, `rolesMap`, `KbEventsByRestrictionRequest`).

# Data Model: Client DTO Records & Mapper Contracts

**Feature**: `004-clean-code-static-mappers`
**Date**: 2026-11-25

## Client DTO Records (`com.biopatternsg.infrastructure.clients.dtos`)

### `NcbiESearchResponse`
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record NcbiESearchResponse(
        @JsonProperty("esearchresult") NcbiESearchResultDto eSearchResult
) {}
```

### `NcbiESearchResultDto`
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record NcbiESearchResultDto(
        @JsonProperty("count") String count,
        @JsonProperty("retmax") String retmax,
        @JsonProperty("retstart") String retstart,
        @JsonProperty("idlist") List<String> idList
) {}
```
*(Verify exact field names in existing `NcbiESearchResultDto.java` during implementation so zero JSON fields are lost.)*

## Pagination Guard (`KbObjectRepositoryAdapter.findByPipelineId`)
- `safePage = Math.max(page, 0)`
- If `size <= 0`: returns empty `PaginatedResult` with `totalPages = 0`, `size = 0`, `page = safePage`, `totalItems = totalItems`.
- Otherwise: `totalPages = (int) Math.ceil((double) totalItems / size)`.

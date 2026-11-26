# Data Model: Outbound Adapters Business-Logic Extraction and Parameter Encapsulation

**Feature**: `003-outbound-adapters-param-encapsulation`
**Date**: 2026-11-25

## New Domain Enums & Utilities

### `RestrictionLevel` (`com.biopatternsg.domain.model`)
Enum representing knowledge-base event visibility filtering:
- `UNRESTRICTED`
- `RESTRICTED`
- `VERY_RESTRICTED`

**Factory Method**: `RestrictionLevel.fromString(String value)`
- `null`, blank, `"NO_RESTRICTED"`, or `"UNRESTRICTED"` (case-insensitive) → `UNRESTRICTED`
- `"RESTRICTED"` (case-insensitive) → `RESTRICTED`
- `"VERY_RESTRICTED"` (case-insensitive) → `VERY_RESTRICTED`
- Any other unrecognized non-blank value → `UNRESTRICTED` (matching previous fallback behavior)

### `BiologicalTermNormalizer` (`com.biopatternsg.domain.util`)
Stateless `public final` utility class with private constructor.
- Nested record `NormalizedTerm(String biotypePrefix, String term)`
- `normalize(String rawTerm)`:
  - If `rawTerm == null || rawTerm.isBlank()` → `new NormalizedTerm(null, rawTerm)`
  - Trims input; if matches `^@([^_]+)_(.+)$`:
    - `biotypePrefix` = `group(1).trim().toUpperCase()`
    - `suffix` = `group(2).trim()` (uses `suffix` if non-blank, otherwise trimmed input)
    - returns `new NormalizedTerm(biotypePrefix, suffix)`
  - Otherwise returns `new NormalizedTerm(null, trimmed)`
- `extractSuffix(String rawTerm)`: returns `normalize(rawTerm).term()`.

## New Domain Parameter Records (`com.biopatternsg.domain.model`)

### `BuildPubmedPairsCommand`
```java
public record BuildPubmedPairsCommand(
        String pipelineId,
        boolean useShortName,
        int levels,
        String userId
) {}
```

### `PipelineStepUpdate`
```java
public record PipelineStepUpdate(
        String pipelineId,
        PipelineSteps step,
        Status status,
        String userId,
        Map<String, String> metrics
) {
    public PipelineStepUpdate {
        metrics = (metrics != null) ? Map.copyOf(metrics) : null;
    }

    public static PipelineStepUpdate of(String pipelineId, PipelineSteps step, Status status, String userId) {
        return new PipelineStepUpdate(pipelineId, step, status, userId, null);
    }

    public static PipelineStepUpdate withMetrics(
            String pipelineId, PipelineSteps step, Status status, String userId, Map<String, String> metrics) {
        return new PipelineStepUpdate(pipelineId, step, status, userId, metrics);
    }
}
```

### `KbEventKey`
```java
public record KbEventKey(
        String pipelineId,
        String first,
        String relation,
        String second
) {}
```

### `KbObjectUpsertCommand`
```java
public record KbObjectUpsertCommand(
        String rawName,
        List<String> synonyms,
        Map<String, String> biotypes,
        List<String> itemBiotypes
) {
    public KbObjectUpsertCommand {
        synonyms = (synonyms != null) ? List.copyOf(synonyms) : List.of();
        biotypes = (biotypes != null) ? Map.copyOf(biotypes) : Map.of();
        itemBiotypes = (itemBiotypes != null) ? List.copyOf(itemBiotypes) : List.of();
    }
}
```

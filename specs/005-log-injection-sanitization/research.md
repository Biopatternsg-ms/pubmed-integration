# Research & Technical Decisions: Log Injection Prevention and Logging Sanitization

**Feature**: `005-log-injection-sanitization`
**Date**: 2026-11-25

## 1. Collection & Exception Message Sanitization

- **Decision**: Add `public static List<String> sanitizeCollection(Collection<?> values)` to `LogSanitizer` (returning `List.of()` for `null`/empty, otherwise `values.stream().map(LogSanitizer::sanitize).toList()`). Wrap all scalar untrusted/external values and `e.getMessage()` formatted arguments in `LogSanitizer.sanitize(...)`, and wrap collections (`pubmedIds`, `processedSynonyms`, `uniqueBiotypes`, `result.ids()`, expert symbols) in `LogSanitizer.sanitizeCollection(...)`.
- **Rationale**: `LogSanitizer.sanitize(Object)` already handles `null` (`"null"`) and replaces `\r` and `\n` with `_`. Adding `sanitizeCollection` provides clean, readable list output in logs and fixes `GenerateAlignedObjectsUseCase` logging an unconsumed `Stream`.
- **Alternatives considered**: Relying on `LogSanitizer.sanitize(collection)` (stringifies entire list) — works, but `sanitizeCollection` avoids `Stream.toString()` bugs and produces a clean `List<String>`.

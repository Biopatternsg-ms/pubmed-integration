# Data Model: LogSanitizer Contract

**Feature**: `005-log-injection-sanitization`
**Date**: 2026-11-25

## `LogSanitizer` (`com.biopatternsg.domain.util`)
- `public static String sanitize(Object value)`: returns `"null"` if `value == null`, otherwise `String.valueOf(value).replace('\r', '_').replace('\n', '_')`.
- `public static List<String> sanitizeCollection(Collection<?> values)`: returns `List.of()` if `values == null`, otherwise `values.stream().map(LogSanitizer::sanitize).toList()`.

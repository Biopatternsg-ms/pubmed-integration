# Contracts: Logging Sanitization Standards

**Feature**: `005-log-injection-sanitization`
**Date**: 2026-11-25

Every formatted SLF4J log statement (`log.info`, `log.warn`, `log.error`, `log.debug`) across `application.*` and `infrastructure.*` MUST wrap untrusted/external inputs (`pipelineId`, `term`, `pmid`, `name`, `restrictionLevel`, `biologicalObject.id()`, `kbEvent` fields, `pmidsParam`, `request`, `result` fields, collections, and formatted `e.getMessage()`) in `LogSanitizer.sanitize(...)` or `LogSanitizer.sanitizeCollection(...)`.

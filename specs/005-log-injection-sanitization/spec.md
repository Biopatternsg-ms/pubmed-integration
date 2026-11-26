# Feature Specification: Log Injection Prevention and Logging Sanitization

**Feature Branch**: `feature/05-log-injection-prevention-and-logging-sanitization`

**Created**: 2026-11-25

**Status**: Draft

**Input**: User description: "/speckit-specify basado en /home/yacson/Proyectos/biopatternsg-ms/SDD/pubmed-integrations/05-log-injection-prevention-and-logging-sanitization"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sanitize All Untrusted and External Inputs in Application Logs (Priority: P1)

As a security engineer and system operator, I want all user-supplied, message-queue, and external-service values written to application logs to be sanitized against newline and carriage-return injection, so that attackers or malformed external payloads cannot forge log entries (CWE-117) and automated security scanners report zero log-injection findings.

**Why this priority**: Unsanitized external values in log statements create a direct log-forging vulnerability (CWE-117) and trigger security analysis alerts across multiple orchestration, messaging, and persistence flows.

**Independent Test**: Can be verified by passing strings and collections containing `\n` and `\r` characters through the log sanitization utility and inspecting all log statements across application, messaging, persistence, and external-integration components.

**Acceptance Scenarios**:

1. **Given** any user-supplied, queue-derived, or external-API-derived identifier or payload (such as pipeline identifiers, search terms, publication identifiers, biological object identifiers, event terms, or error messages), **When** it is logged at any severity level, **Then** newline (`\n`) and carriage-return (`\r`) characters are neutralized before reaching the log output.
2. **Given** a collection of untrusted or external values (such as publication identifier lists, synonyms, or biotypes) is logged, **When** formatted for logging, **Then** each element or its string representation is sanitized consistently without exposing raw control characters.

---

### User Story 2 - Fix Unconsumed Stream Logging in Object Alignment Diagnostics (Priority: P2)

As a pipeline operator reviewing diagnostic logs, I want expert-object alignment logs to display the actual sanitized biological symbol names rather than an internal stream object reference, so that operational logs are human-readable and safe.

**Why this priority**: Logging an unconsumed stream reference prints an opaque memory address instead of useful diagnostic symbols, hindering troubleshooting during alignment runs.

**Independent Test**: Can be verified by executing the alignment flow with sample expert objects and confirming the logged symbol list contains readable, sanitized symbol names.

**Acceptance Scenarios**:

1. **Given** an alignment request containing expert biological objects (including symbols with special or newline characters), **When** the alignment flow logs the incoming objects, **Then** the log outputs a concrete list of sanitized symbol strings rather than a stream object reference.

---

### Edge Cases

- What happens when a logged value or collection is `null` or empty? Sanitization handles `null` and empty inputs gracefully without throwing null-pointer exceptions.
- What happens when an exception message (`e.getMessage()`) contains newline characters from an upstream service or malformed payload? Either the message is omitted when the full exception is already attached, or it is explicitly sanitized before formatting.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST sanitize every user-provided, message-queue, and external-API value (replacing `\r` and `\n` characters with safe substitutes) before writing it to any application log.
- **FR-002**: System MUST sanitize or safely format exception messages whenever logged as separate formatted arguments alongside exception traces.
- **FR-003**: System MUST provide null-safe sanitization support for both scalar values and collections of values, verified by automated unit tests.
- **FR-004**: System MUST log a concrete, sanitized list of expert object symbols during alignment initialization instead of an unconsumed stream reference.
- **FR-005**: System MUST pass full compilation, architecture-governance, documentation-governance, and unit test suites after all logging updates.

### Key Entities

- **Log Sanitizer**: Pure domain utility responsible for neutralizing line-break characters (`\r`, `\n`) in scalar values and collections prior to logging.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of formatted log statements receiving external, user-supplied, or queue-derived values apply sanitization before logging.
- **SC-002**: 0 unconsumed stream object references appear in diagnostic log statements.
- **SC-003**: 100% of sanitization unit tests (covering `\r`, `\n`, `null`, and collections) and full project build/governance checks pass with zero regressions.

## Assumptions

- Existing logging backend configuration and global log levels remain unchanged.
- Existing REST, RabbitMQ, and persistence schemas remain unchanged.

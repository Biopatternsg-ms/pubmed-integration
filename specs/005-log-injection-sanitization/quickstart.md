# Quickstart Validation Guide

**Feature**: `005-log-injection-sanitization`
**Date**: 2026-11-25

## Validation Steps

1. **Compile**:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw clean compile
   ```
2. **Run Architecture & Documentation Governance Tests**:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest
   ```
3. **Run Full Unit Test Suite** (including `LogSanitizerTest`):
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test
   ```
4. **Verify Zero Unsanitized External Log Statements**:
   Inspect `grep -rn "log\.\(info\|warn\|error\|debug\)(" src/main/java/com/biopatternsg/` to confirm all untrusted/external arguments are sanitized.

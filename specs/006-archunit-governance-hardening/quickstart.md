# Quickstart Validation Guide

**Feature**: `006-archunit-governance-hardening`
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
   Expected: **14 tests run** (13 in `ArchitectureLayerTest`, 1 in `DocumentationGovernanceTest`), 0 failures, 0 errors, 0 skipped.
3. **Run Full Unit Test Suite**:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test
   ```
   Expected: All tests pass with zero errors.

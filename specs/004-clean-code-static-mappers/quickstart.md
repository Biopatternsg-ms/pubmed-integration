# Quickstart Validation Guide

**Feature**: `004-clean-code-static-mappers`
**Date**: 2026-11-25

## Prerequisites

- Java 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`)
- Working directory: `services/pubmed-integration`

## Validation Steps

1. **Compile**:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw clean compile
   ```
   Expected: `BUILD SUCCESS`.

2. **Run Architecture & Documentation Governance Tests**:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest
   ```
   Expected: All tests pass.

3. **Run Full Unit Test Suite**:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test
   ```
   Expected: All existing and new unit tests (static mappers, null-safety `alignedAs`, `KbObjectRepositoryAdapter` pagination bounds, `NcbiESearchResponse` / `NcbiESearchResultDto` `toString()`) pass with zero failures.

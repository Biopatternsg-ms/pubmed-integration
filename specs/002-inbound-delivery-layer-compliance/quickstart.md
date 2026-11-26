# Quickstart & Validation Guide: Inbound Delivery-Layer Compliance

**Feature**: `002-inbound-delivery-layer-compliance` | **Date**: 2026-11-24

## Prerequisites

- Java 21 (`JAVA_HOME` pointing to JDK 21)
- Maven Wrapper (`./mvnw`) in `services/pubmed-integration`

## Validation Steps

1. **Compile with Java 21**:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw clean compile
   ```
   Expected: `BUILD SUCCESS` with zero errors.

2. **Run Architecture & Documentation Governance Tests**:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest
   ```
   Expected: All ArchUnit rules and Use Case documentation governance checks (`ProcessNcbiSearchRequestUseCase.md`, `ProcessPubtatorBatchUseCase.md`, `README.md`, `architecture.md` Section 5) pass.

3. **Run Full Unit Test Suite**:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test
   ```
   Expected: All tests pass (including new tests for `ProcessNcbiSearchRequestUseCase`, `ProcessPubtatorBatchUseCase`, `SessionUtils`, and `AppLifecycleBean`).

4. **Verify Controller & Consumer Cleanliness**:
   ```bash
   grep -rn "buildTreeMesh" src/main/java/ || echo "NO_BUILD_TREE_MESH"
   ```
   Expected: `NO_BUILD_TREE_MESH`.

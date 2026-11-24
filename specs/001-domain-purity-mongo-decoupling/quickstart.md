# Quickstart & Validation Guide: Domain and Application Layer Purity

**Feature**: `001-domain-purity-mongo-decoupling` | **Date**: 2026-11-24

## Prerequisites

- Java 21 active on `PATH` / `JAVA_HOME`
- Maven Wrapper (`./mvnw`) in `services/pubmed-integration`

## Validation Steps

1. **Verify compilation**:
   ```bash
   ./mvnw clean compile
   ```
   Expected: Build succeeds with zero compilation errors.

2. **Verify Hexagonal Architecture & Documentation Governance gates**:
   ```bash
   ./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest
   ```
   Expected: All ArchUnit layer isolation, naming convention, and documentation governance tests pass.

3. **Verify zero forbidden imports in `domain` and `application`**:
   ```bash
   grep -rnE "com\.biopatternsg\.mongo|com\.fasterxml\.jackson|jakarta\.ws\.rs|io\.quarkus|io\.vertx|org\.bson|com\.cifertech" src/main/java/com/biopatternsg/domain/
   grep -rnE "com\.biopatternsg\.mongo|com\.biopatternsg\.infrastructure|com\.cifertech" src/main/java/com/biopatternsg/application/
   ```
   Expected: Both commands return zero matches (exit code 1).

4. **Verify no classes remain in `com.biopatternsg.mongo`**:
   ```bash
   ls -la src/main/java/com/biopatternsg/mongo/ 2>/dev/null
   ```
   Expected: Directory does not exist; all 9 entities reside in `src/main/java/com/biopatternsg/infrastructure/mongo/`.

5. **Run full unit test suite**:
   ```bash
   ./mvnw test
   ```
   Expected: All unit tests pass with zero failures or errors.

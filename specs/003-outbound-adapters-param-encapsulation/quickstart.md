# Quickstart Validation Guide

**Feature**: `003-outbound-adapters-param-encapsulation`
**Date**: 2026-11-25

## Prerequisites

- Java 21 active on `PATH` (`java -version`)
- Working directory: `services/pubmed-integration`

## Validation Steps

1. **Compile and refresh classes**:
   ```bash
   ./mvnw clean compile
   ```
   Expected: `BUILD SUCCESS` with zero compilation errors.

2. **Run Architecture & Documentation Governance Tests**:
   ```bash
   ./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest
   ```
   Expected: Both test classes pass, confirming hexagonal layer isolation, naming conventions, and synchronized `.agents/use_cases/*.md` documentation.

3. **Run Full Unit Test Suite**:
   ```bash
   ./mvnw test
   ```
   Expected: All unit tests pass, including:
   - `BiologicalTermNormalizerTest` (`@PREFIX_SUFFIX`, multiple underscores, plain terms, blank/null)
   - `RestrictionLevelTest` (if added) & `GetKbEventsByRestrictionUseCaseTest` (verifying `UNRESTRICTED`, `NO_RESTRICTED`, `RESTRICTED`, `VERY_RESTRICTED`, blank/null)
   - `PubmedPairsBuilderServiceImplTest` & `BuildPubmedPairsUseCaseTest` (including concurrent parallel-stream safety)
   - `ExpertObjectAlignmentServiceImplTest` & `GenerateAlignedObjectsUseCaseTest`
   - `KbEventRepositoryAdapterTest`, `KbObjectRepositoryAdapterTest`, `GenerateKbForPipelineUseCaseTest`, `ProcessNcbiSearchRequestUseCaseTest`, `ProcessPubtatorBatchUseCaseTest`

4. **Verify Parameter Count Compliance**:
   Verify no method in `domain.ports.*`, `application.usecase.*`, `application.services.*`, or `infrastructure.adapters.*` accepts more than 3 parameters.

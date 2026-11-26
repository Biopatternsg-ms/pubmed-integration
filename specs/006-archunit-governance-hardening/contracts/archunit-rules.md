# Contracts: ArchUnit Governance Rules (`ArchitectureLayerTest`)

**Feature**: `006-archunit-governance-hardening`
**Date**: 2026-11-25

Existing 7 rules preserved + 6 new `@Test` methods added to `archtest.ArchitectureLayerTest`:
1. `domainShouldNotDependOnFrameworksTest`
2. `applicationShouldNotDependOnDeliveryOrPersistenceFrameworksTest`
3. `useCasesShouldNotDependOnServiceImplementationsTest`
4. `staticMappersShouldBeStatelessFinalUtilityClassesTest`
5. `noFieldInjectionInApplicationAndAdaptersTest`
6. `inboundAdaptersShouldNotDependOnOutboundPortsTest`

# Data Model: Governed Packages & Rules

**Feature**: `006-archunit-governance-hardening`
**Date**: 2026-11-25

No runtime domain entities are introduced. The governed architectural units are:
- **Domain (`com.biopatternsg.domain..`)**: Zero dependencies on `jakarta.ws.rs..`, `jakarta.enterprise..`, `jakarta.inject..`, `jakarta.xml.bind..`, `com.fasterxml.jackson..`, `io.quarkus..`, `io.vertx..`, `org.bson..`, `org.eclipse.microprofile..`, `org.w3c.dom..`, `com.cifertech..`.
- **Application (`com.biopatternsg.application..`)**: Zero dependencies on `jakarta.ws.rs..`, `io.vertx..`, `io.quarkus.mongodb..`, `org.bson..`, `com.cifertech..`.
- **Use Cases (`com.biopatternsg.application.usecase..`)**: Zero dependencies on `com.biopatternsg.application.services.impl..`.
- **Mappers (`..infrastructure.mapper..`)**: Top-level classes must be `final`, have only `private` constructors, and carry no `@ApplicationScoped`, `@Singleton`, or `@RequestScoped` annotations.
- **Fields in `application..` & `infrastructure.adapters..`**: Must not be annotated with `@Inject` or `@ConfigProperty`.
- **Inbound Adapters (`..infrastructure.adapters.in.restcontrollers..`, `..infrastructure.adapters.in.consumers..`)**: Zero dependencies on `com.biopatternsg.domain.ports.out..`.

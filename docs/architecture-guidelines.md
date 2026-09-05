# Architecture Guidelines

## Architecture

Use lightweight **Hexagonal Architecture**.

```text
REST Adapter
     ↓
Application / Use Cases
     ↓
Domain
     ↑
Repository Port
     ↑
Persistence Adapter
     ↓
PostgreSQL
```

Dependencies point inward. Domain code must not depend on Spring, HTTP, JPA, or database implementation details.

## Domain

Business rules belong in the domain.

Interest calculation should be extensible. Prefer a **Strategy / Policy** approach instead of growing conditional blocks.
Plan-specific interest rules are implemented as explicit, framework-independent policies.

Adding a new plan type should require minimal changes to existing code.

## Application

Application services represent the two use cases:

- Get all time deposits
- Update all time deposit balances

They orchestrate domain logic and persistence but must not contain interest calculation rules.

## API

Expose exactly **two business REST endpoints**.

Controllers should only:

- accept requests
- invoke application use cases
- map responses

The current REST controller is a thin inbound adapter over the two application ports.

Use OpenAPI / Swagger.

## Persistence

Use PostgreSQL with explicit Flyway migrations.

Prefer:

- JPA / Spring Data in the persistence adapter
- Flyway for schema migrations
- Testcontainers for integration tests
- Docker Compose for the local application and PostgreSQL runtime

Do not expose JPA entities or Spring Data repositories to the domain.

The database schema must stay consistent with `docs/erd.puml`.

Persistence infrastructure belongs outside the domain. Database configuration, migrations,
JPA mappings, and repository implementations must not leak into domain classes.

## Current Package Skeleton

The Kotlin implementation uses this lightweight package layout under `org.ikigaidigital`:

```text
domain/
application/
  port/
    in/
    out/
  service/
adapter/
  in/
    rest/
  out/
    persistence/
```

The current implementation includes the outbound Spring Data JPA persistence adapter,
application services for the two use cases, and thin REST controllers for the two
documented business endpoints.

## Money

Use `BigDecimal` for all monetary values.

Never use `Double` or `Float` for balances, withdrawals, or interest.

Preserve existing rounding behavior unless requirements explicitly require otherwise.

## Atomicity and Transactions

Updating all balances is one business operation and should have a clear transaction boundary.

Prefer database transactions to in-memory coordination when consistency depends on persisted state.

Rules:

- Keep transaction boundaries at the application/use-case level.
- Avoid partial updates if the operation is expected to succeed or fail as one unit.
- Do not introduce distributed transactions for a single-database application.
- Do not add Saga, TCC, 2PC, outbox, or similar patterns unless requirements justify them.
- Preserve domain invariants inside the transaction.

## Concurrency and Locking

Do not add locks preemptively.

Start with the database consistency model and only introduce locking when there is a concrete race condition.

Prefer, in order:

1. Database transaction guarantees.
2. Optimistic locking when concurrent updates are expected but conflicts are rare.
3. Pessimistic locking only when a real invariant requires serialization.
4. Application/distributed locks only when database-level coordination is insufficient.

Rules:

- Never use a global JVM lock for database-backed business state.
- Do not rely on `synchronized` for cross-instance consistency.
- Keep lock scope as small as possible.
- Avoid holding locks during network calls or slow operations.
- Document the invariant protected by every explicit lock.

## Scale

Design for the stated problem, not hypothetical internet scale.

Prefer simple single-service + relational database architecture unless requirements prove otherwise.

Do not introduce:

- Kafka
- distributed cache
- sharding
- event sourcing
- CQRS
- microservices
- distributed locks

without a concrete requirement.

When discussing scale, first identify:

- expected data size
- read/write pattern
- transaction boundaries
- contention points
- database bottlenecks

Scale vertically and with database/index/query improvements before adding distributed-system complexity.

## Security

Security first, but complexity must be justified.

- Keep the dependency surface minimal.
- Prefer Spring Boot BOM-managed dependency versions.
- Review dependency, build, infrastructure, API-boundary, and persistence changes for security impact.
- Use OWASP Dependency-Check and Maven dependency tree analysis for dependency vulnerability management.
- Keep secrets and environment-specific configuration out of source control.
- Validate untrusted input at system boundaries.
- Use safe, parameterized persistence access.
- Avoid logging secrets or exposing unnecessary internal exception details through APIs.
- Use least privilege where practical for database and runtime access.

Do not add service mesh, WAF, Vault, SIEM, external IAM platforms, or dedicated security
infrastructure unless explicit requirements justify the complexity.

## Legacy Behavior

`TimeDepositCalculator.updateBalance` is protected existing behavior.

Before refactoring:

1. Understand current behavior.
2. Protect it with characterization tests.
3. Keep the existing method signature.
4. Preserve observable behavior.

Prefer extracting responsibilities over rewriting working logic.

## Testing

Use:

- Characterization tests for existing behavior
- Unit tests for domain rules
- Testcontainers for persistence
- Integration tests for the two required API endpoints

Pay special attention to business boundaries such as:

- 30 days
- 45 days
- 1 year

For concurrency-sensitive code, test the invariant rather than implementation details.

Unit and characterization tests must not require PostgreSQL, Docker, Spring context startup,
or other infrastructure. Persistence integration tests should use real PostgreSQL through
Testcontainers and real Flyway migrations.

## Build Reproducibility

Use Maven with the repository Maven Wrapper for reproducible local and CI builds.

Expected project commands are run from `kotlin/`:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

## Engineering Principles

Prefer:

- SOLID
- constructor injection
- explicit dependencies
- immutable values where practical
- small focused classes
- simple, readable Kotlin

Avoid:

- speculative abstractions
- unnecessary frameworks
- framework dependencies in the domain
- clever Kotlin that reduces readability
- unrelated refactoring
- overengineering

When two designs satisfy the requirements, choose the simpler one.

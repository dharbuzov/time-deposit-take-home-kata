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

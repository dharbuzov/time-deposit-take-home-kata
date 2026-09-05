# 07 - Persistence Adapter and Database Mapping

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- `docs/erd.puml`
- current hexagonal package structure
- current Flyway migrations
- current Spring Boot configuration
- current tests

Inspect the repository before making changes.

Assume:

- characterization tests are complete
- infrastructure baseline is complete
- OpenAPI contract is aligned
- security baseline is complete
- hexagonal architecture skeleton is complete

## Goal

Implement the outbound persistence adapter for time deposits and withdrawals using PostgreSQL and Spring Data JPA, while preserving the existing database schema exactly as required by the assignment.

This step is persistence-focused.

Do not implement REST endpoint behavior yet.
Do not refactor the legacy interest calculation yet.
Do not change the OpenAPI contract.
Do not redesign the database schema.

## 1. Database Schema Is Fixed

Treat the schema documented in `README.md` as authoritative.

Preserve the required table and column names exactly.

Required tables include:

- `timeDeposits`
- `withdrawals`

Required fields include:

### `timeDeposits`

- `id`
- `planType`
- `days`
- `balance`

### `withdrawals`

- `id`
- `timeDepositId`
- `amount`
- `date`

Do not rename these to snake_case.

Do not introduce alternative naming conventions.

Do not add speculative columns such as:

- `version`
- `createdAt`
- `updatedAt`
- `status`
- audit columns
- soft-delete flags

unless explicitly required by the assignment.

If `docs/erd.puml` conflicts with README, update the ERD documentation rather than changing the required schema.

## 2. Flyway Alignment

Review the existing Flyway migration.

Ensure the migration creates exactly the required schema.

Use database types appropriate for the assignment:

- integer IDs
- string plan type
- integer days
- decimal monetary values
- date withdrawal date
- foreign key from `withdrawals.timeDepositId` to `timeDeposits.id`

Do not add extra schema features without a concrete requirement.

If the existing migration is already correct, do not rewrite it unnecessarily.

If it is incorrect, make the smallest safe correction appropriate for the current repository state.

## 3. JPA Persistence Entities

Create persistence-specific JPA entities under the outbound persistence adapter.

Do not annotate the protected legacy `TimeDeposit` class with JPA annotations.

The persistence entities must remain infrastructure concerns.

Create mappings for:

- time deposit
- withdrawal

Map explicitly to the required table and column names.

Do not rely on automatic naming conventions where they could silently convert camelCase names to snake_case.

Use explicit table/column annotations where needed.

## 4. Money Representation

Use `BigDecimal` for new persistence monetary fields:

- time deposit balance
- withdrawal amount

Do not use `Double` or `Float` in new persistence code for money.

Do not change the protected legacy `TimeDeposit.balance` type.

Any conversion between persistence `BigDecimal` and legacy `Double` must be explicit and isolated.

Do not spread legacy monetary representation into new persistence models.

## 5. Relationship Mapping

Model the required relationship:

```text
timeDeposits 1 -> many withdrawals
```

Keep the mapping simple.

Avoid unnecessary bidirectional JPA relationships.

Prefer the simplest mapping that supports the required GET response and persistence behavior.

Be careful with:

- eager loading
- N+1 queries
- orphan removal
- cascading writes

Do not enable broad cascading by default.

Only use cascade behavior when there is a concrete persistence requirement.

## 6. Outbound Port Implementation

Implement the persistence adapter against the outbound application port introduced in the previous step.

The application layer must not depend directly on:

- Spring Data repositories
- JPA entities
- Hibernate APIs

Keep framework types inside the adapter.

The adapter should expose only operations needed by the current application use cases.

Do not create a generic CRUD abstraction.

## 7. Spring Data Repositories

Create internal Spring Data repository interfaces as implementation details of the persistence adapter.

Keep them inside the outbound adapter package.

Do not expose Spring Data repository interfaces to the application layer.

Use only the repository methods actually required.

Avoid speculative custom queries.

## 8. Persistence Mapping

Create explicit mapping between:

- persistence entities
- application/domain/read models

Do not return JPA entities directly from application ports.

Do not reuse REST DTOs as JPA entities.

Do not reuse persistence entities as domain models.

Keep mappings readable and small.

Avoid introducing a mapping framework such as MapStruct unless there is a concrete need.

Prefer simple Kotlin mapping functions/classes for this assignment.

## 9. Withdrawals in Read Model

The persistence adapter must be capable of loading the withdrawal data required for the GET API response.

The resulting application/read model should be able to represent:

- deposit id
- plan type
- balance
- days
- withdrawals

Each withdrawal should contain the fields required by `docs/openapi.yaml`.

Do not modify the legacy `TimeDeposit` class to add withdrawals.

## 10. Persistence Tests with Testcontainers

Add focused persistence integration tests using the existing PostgreSQL Testcontainers setup.

Tests should verify at minimum:

- Flyway schema starts correctly
- a time deposit can be persisted/read
- withdrawals are associated with the correct time deposit
- decimal monetary values round-trip correctly
- retrieval returns withdrawals correctly
- multiple deposits can be retrieved

Use real PostgreSQL.

Do not use H2.

Avoid testing Spring Data itself.

Test the persistence adapter behavior and mapping.

## 11. Test Data

Use small, explicit test fixtures.

Include representative values for:

- Basic
- Student
- Premium
- deposits with no withdrawals
- deposits with one or more withdrawals
- decimal balances/amounts

Do not introduce a large fixture framework.

## 12. Transaction Boundaries

Do not implement the final update-all transaction orchestration in this step.

Persistence operations should be transaction-compatible, but the application use case will own the business transaction boundary in a later step.

Do not add distributed transaction mechanisms.

Do not add Saga, TCC, 2PC, queues, or distributed locks.

## 13. Concurrency

Do not add optimistic locking or pessimistic locking unless a concrete invariant in the current assignment requires it.

Do not add a `version` column.

Do not add JVM-level synchronization for database state.

The baseline should rely on normal database transaction guarantees until the update-all use case is implemented and reviewed.

## 14. Security First

Follow the repository Security First rules.

For persistence:

- do not build SQL from untrusted input
- prefer Spring Data/JPA parameter binding
- do not log database credentials
- do not hardcode production credentials
- do not expose persistence internals through API/domain boundaries

If dependencies or build configuration are changed, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

Do not add new dependencies unless required.

## 15. Documentation

Update documentation only to reflect what is actually implemented.

### README.md

Update persistence/testing sections if needed to describe:

- PostgreSQL persistence
- Flyway
- JPA adapter
- Testcontainers persistence integration tests

Do not duplicate implementation details excessively.

### docs/architecture-guidelines.md

Update only if necessary to reflect the actual adapter/mapping approach.

### docs/erd.puml

Ensure it matches the authoritative README schema exactly.

Do not modify `docs/openapi.yaml` in this step.

## Verification

After implementation:

1. Run:

```bash
./mvnw clean test
```

2. Confirm characterization tests still pass.

3. Confirm PostgreSQL Testcontainers tests pass.

4. Confirm Flyway migration succeeds.

5. Confirm JPA mappings use the exact required table/column names.

6. Confirm money uses `BigDecimal` in new persistence code.

7. Confirm legacy `TimeDeposit` remains unchanged.

8. Confirm application layer does not import JPA/Spring Data types.

9. Confirm no REST endpoint behavior was added.

10. Confirm no extra schema columns/tables were introduced.

11. If dependencies changed, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

## Constraints

- README schema is authoritative.
- Preserve exact required table and column names.
- Do not convert camelCase schema names to snake_case.
- Do not add speculative schema fields.
- Do not change `TimeDeposit`.
- Do not change `TimeDepositCalculator.updateBalance` signature.
- Do not change legacy calculator behavior.
- Do not change `docs/openapi.yaml`.
- Do not implement REST endpoint behavior yet.
- Do not refactor interest calculation yet.
- Do not add locking/versioning without a concrete requirement.
- Keep JPA inside the persistence adapter.
- Use `BigDecimal` for new monetary persistence code.
- Prefer simple explicit mappings.
- Preserve Security First rules.

## Output

Before editing, provide a short persistence plan.

After implementation, report:

1. Files changed
2. Flyway/schema changes, if any
3. JPA entities added
4. Spring Data repositories added
5. Persistence adapter implementation
6. Mapping approach
7. Withdrawal relationship approach
8. Monetary representation
9. Tests added
10. Test results
11. Security scan results if dependencies changed
12. Documentation updates
13. Confirmation that schema names, OpenAPI contract, and legacy behavior were preserved
14. Design decisions intentionally deferred to the next step

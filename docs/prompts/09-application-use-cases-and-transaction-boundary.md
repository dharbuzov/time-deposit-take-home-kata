# 09 - Application Use Cases and Transaction Boundary

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- current domain model
- current interest policies
- current application ports
- current persistence adapter
- current tests

Inspect the repository before making changes.

Assume:

- characterization tests are complete
- infrastructure baseline is complete
- OpenAPI contract is aligned
- security baseline is complete
- hexagonal architecture skeleton is complete
- persistence adapter is complete
- interest calculation refactoring is complete

## Goal

Implement the two application use cases required by the assignment:

1. retrieve all time deposits
2. update balances of all persisted time deposits

This step owns orchestration and transaction boundaries.

Do not implement REST controllers yet.
Do not change the OpenAPI contract.
Do not change the database schema.
Do not change legacy calculator semantics.

## 1. Exactly Two Application Use Cases

Implement exactly the two business capabilities already defined by the inbound ports.

### Get all time deposits

The use case should:

- load all persisted time deposits
- include their withdrawals
- map them into the application/read model required by the API layer
- return no persistence/JPA types

Do not add:

- filtering
- pagination
- search
- CRUD variants
- withdrawal-specific use cases

unless explicitly required by the assignment.

### Update all balances

The use case should:

- load all persisted time deposits required for the operation
- apply the existing interest calculation behavior
- persist the updated balances
- complete as one clear business operation

Do not add request parameters unless required by the assignment.

## 2. Transaction Boundary

Treat update-all as one business operation.

Place the transaction boundary at the application service/use-case level.

Prefer a Spring transaction annotation/configuration on the application orchestration boundary rather than inside:

- domain policies
- JPA entities
- REST controllers
- repository implementation details

The goal is to avoid partially updated deposits if persistence fails during the operation.

Keep the transaction boundary explicit and easy to explain.

## 3. Preserve Existing Calculator Behavior

The application use case must reuse the refactored calculation path that preserves:

- protected `TimeDeposit` contract
- `TimeDepositCalculator.updateBalance` signature
- existing mutation semantics
- plan matching behavior
- unknown-plan behavior
- exact day boundaries
- existing rounding behavior

Do not reimplement the interest rules independently in the application service.

There must be one source of truth for interest behavior.

## 4. Legacy Compatibility Mapping

If the application/persistence model uses `BigDecimal` while the protected legacy model uses `Double`, keep conversions explicit and isolated.

The orchestration may conceptually perform:

```text
persisted model
    ↓
legacy-compatible calculation model
    ↓
TimeDepositCalculator
    ↓
updated legacy-compatible values
    ↓
persisted model
```

Do not spread conversion logic across controllers, repositories, and policies.

Keep the compatibility mapping small and testable.

## 5. Money Safety

New application/persistence monetary models should continue to use `BigDecimal`.

Do not change the legacy `TimeDeposit.balance` type.

Do not introduce additional floating-point arithmetic outside the protected compatibility path.

Verify that persistence round-tripping does not change the characterized calculator result.

## 6. Repository Port Usage

Application services must depend only on outbound ports.

Do not inject Spring Data repositories directly into application services.

Do not expose JPA entities outside the persistence adapter.

Use only persistence operations required by the two use cases.

Avoid generic CRUD abstractions.

## 7. Update Persistence Strategy

Choose the simplest persistence approach that preserves correctness.

For the update-all use case:

- load the required deposits
- calculate updated balances
- persist updates inside the same transaction

Do not introduce batching complexity unless there is a demonstrated need.

Do not add queues, async jobs, event sourcing, CQRS, Saga, TCC, or 2PC.

## 8. Concurrency

Do not introduce distributed locking.

Do not add a version column solely for theoretical concurrency concerns.

Do not add JVM `synchronized` around database state.

Use the existing database transaction guarantees as the baseline.

If the current implementation has a concrete lost-update risk that affects the assignment semantics, document it and propose the smallest database-level mitigation before implementing additional locking.

Do not overengineer hypothetical high-scale concurrency.

## 9. Failure Atomicity

Add a focused test demonstrating the intended transaction behavior for update-all.

If one persistence operation fails during the update operation, the transaction should not leave a partially updated set of time deposits.

Prefer testing the application transaction boundary against real PostgreSQL/Testcontainers if practical.

Do not mock away the transaction behavior if the goal of the test is to prove rollback.

## 10. Get-All Read Model

The get-all use case must return all fields needed by the OpenAPI response:

- `id`
- `planType`
- `balance`
- `days`
- `withdrawals`

Do not return JPA entities.

Do not force the legacy `TimeDeposit` object to carry withdrawals.

Keep the read model suitable for later REST mapping.

## 11. Tests

Add focused tests for application orchestration.

Cover at minimum:

### Get all

- empty database/result
- multiple deposits
- withdrawals included
- monetary values preserved

### Update all

- empty database
- multiple plan types
- balances updated according to existing calculator behavior
- resulting values persisted
- withdrawals remain unchanged
- rollback / no partial update on failure

Do not duplicate every interest-policy boundary test here.

Those belong to characterization/domain tests.

Application tests should focus on orchestration.

## 12. Spring Wiring

Wire the application services to the persistence adapter using constructor injection.

Keep dependency direction inward.

Avoid field injection.

Do not make domain policies Spring-dependent unless there is a concrete reason.

## 13. Security First

Follow the repository Security First rules.

For application orchestration:

- do not log sensitive persistence data unnecessarily
- do not expose internal persistence exceptions directly to API callers
- do not add dependencies without need
- preserve safe database access through the persistence adapter

If dependencies/build configuration change, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

## 14. Documentation

Update:

- `README.md`
- `docs/architecture-guidelines.md`

only where needed to reflect:

- the two application use cases
- the update-all transaction boundary
- rollback/atomicity behavior

Keep documentation concise.

Do not change `docs/openapi.yaml`.

Do not change `docs/erd.puml` unless an existing documentation inconsistency is discovered.

## Verification

After implementation:

1. Run:

```bash
./mvnw clean test
```

2. Confirm characterization tests remain unchanged and green.

3. Confirm persistence integration tests remain green.

4. Confirm application use-case tests pass.

5. Confirm update-all is executed within one clear transaction boundary.

6. Confirm rollback prevents partial updates.

7. Confirm application services depend on outbound ports, not Spring Data repositories.

8. Confirm no JPA entities leak into the application/API model.

9. Confirm no extra business use cases were added.

10. Confirm OpenAPI contract and database schema were unchanged.

11. If dependencies changed, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

## Constraints

- Exactly two business use cases.
- Preserve `TimeDeposit`.
- Preserve `TimeDepositCalculator.updateBalance` signature.
- Preserve all characterized calculator behavior.
- Use one clear application-level transaction boundary for update-all.
- Do not implement REST controllers yet.
- Do not change `docs/openapi.yaml`.
- Do not change required database table or column names.
- Do not introduce distributed transactions.
- Do not introduce speculative locks.
- Do not add generic CRUD abstractions.
- Keep JPA inside the persistence adapter.
- Keep new monetary code on `BigDecimal`.
- Preserve Security First rules.

## Output

Before editing, provide a short orchestration/transaction plan.

After implementation, report:

1. Files changed
2. Use cases implemented
3. Application services implemented
4. Repository port operations used
5. Transaction boundary
6. Legacy compatibility mapping
7. Money conversion approach
8. Tests added
9. Rollback/atomicity test result
10. Full test result
11. Security scan result if dependencies changed
12. Documentation updates
13. Confirmation that OpenAPI, schema, and legacy behavior were preserved
14. Design decisions intentionally deferred to the REST adapter step

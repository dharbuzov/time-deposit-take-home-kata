# 13 - End-to-End, Contract, and Integration Testing

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- current Flyway migrations
- current persistence adapter
- current application use cases
- current REST adapters
- current logging/correlation ID implementation
- current tests

Inspect the repository before making changes.

Assume:

- characterization tests are complete
- infrastructure baseline is complete
- OpenAPI contract is aligned
- security baseline is complete
- hexagonal architecture is in place
- persistence adapter is complete
- interest refactoring is complete
- application use cases are complete
- REST adapters are complete
- logging/observability baseline is complete
- utility/shared-code cleanup is complete

## Goal

Add a small but strong end-to-end and integration test suite that proves the complete application works through its real boundaries.

The main flow to verify is:

```text
HTTP
  ↓
REST Adapter
  ↓
Application Use Case
  ↓
Domain / Interest Calculation
  ↓
Persistence Adapter
  ↓
PostgreSQL
```

Use the real Spring application context and PostgreSQL via Testcontainers.

Avoid mocks for tests whose purpose is to prove integration between layers.

Do not change business behavior simply to make tests easier.

## 1. Test Strategy

Keep the test pyramid intentional.

Existing tests should continue to cover:

- legacy characterization
- domain interest behavior
- application orchestration
- persistence adapter behavior
- REST/API behavior

This step adds acceptance-level confidence around the full system.

Do not duplicate every lower-level test at E2E level.

Focus on critical user-visible scenarios.

## 2. PostgreSQL Testcontainers

Use PostgreSQL Testcontainers for integration/E2E tests.

Do not use H2 as a substitute.

The tests must exercise:

- the real database dialect
- Flyway migrations
- JPA mappings
- repository adapter
- transaction behavior

Reuse existing Testcontainers infrastructure if already present.

Avoid starting a new container per individual test unless the existing design requires it.

Prefer a shared test container lifecycle with isolated database state between tests.

## 3. Flyway Verification

Ensure E2E/integration tests start from the real Flyway schema.

The tests should fail if:

- migration is invalid
- required table is missing
- required column name is wrong
- JPA mapping disagrees with schema

Do not create test-only schema that bypasses Flyway.

Required schema naming remains authoritative:

```text
timeDeposits
withdrawals
planType
timeDepositId
```

Do not silently switch to snake_case.

## 4. E2E Scenario - Empty Database

Test the application with an empty database.

### GET

Call:

```text
GET /time-deposits
```

Verify:

- HTTP 200
- JSON array
- empty result
- correlation ID response header exists

### Update balances

Call the configured update-all endpoint from `docs/openapi.yaml`.

Verify:

- contract-defined success status
- no failure when zero deposits exist
- database remains empty

## 5. E2E Scenario - Retrieve Deposits

Seed multiple deposits and withdrawals directly through an appropriate test fixture/persistence mechanism.

Call:

```text
GET /time-deposits
```

Verify the public response includes exactly the contract-required structure:

- `id`
- `planType`
- `balance`
- `days`
- `withdrawals`

Verify withdrawal objects contain the contract-defined fields.

Verify:

- multiple deposits are returned
- withdrawals belong to the correct deposit
- balances are serialized as JSON numbers
- no JPA/internal fields leak into the response

Do not depend on unspecified row ordering unless the contract explicitly defines it.

## 6. E2E Scenario - Update Then Read

This is the primary full-flow test.

Seed deposits representing multiple plan types.

Include meaningful boundaries but do not duplicate the entire characterization suite.

For example, include representative cases such as:

- Basic eligible for interest
- Student eligible for interest
- Premium before or after its eligibility threshold
- unknown plan if useful for compatibility verification

Then:

1. capture original persisted balances
2. call the update-all HTTP endpoint
3. verify the success status
4. call `GET /time-deposits`
5. verify the returned balances reflect the existing calculator behavior
6. verify the values are persisted in PostgreSQL

The test should prove that the HTTP write is not merely changing in-memory state.

## 7. Withdrawals Must Remain Unchanged

For at least one deposit with withdrawals:

1. seed withdrawals
2. call update-all
3. read the data again
4. confirm withdrawals were not modified or deleted

The update-all business operation only updates balances.

## 8. Transaction / Rollback Integration

Preserve the existing application-level rollback test.

If current rollback coverage relies only on mocks, add one real integration test proving transaction atomicity against PostgreSQL.

The test should demonstrate that a failure during the update operation does not leave a partially updated set.

Use the smallest reliable fault-injection mechanism available.

Do not introduce production behavior purely for testing.

Acceptable approaches may include:

- a test-specific failing persistence adapter/wrapper wired into a dedicated integration test
- a controlled database constraint failure if it can be introduced without changing production schema
- another small Spring test configuration that fails partway through persistence

The test must exercise the real transaction manager and PostgreSQL.

Avoid fragile hacks.

## 9. Correlation ID E2E

Verify HTTP correlation behavior through the real endpoint.

### Supplied ID

Send:

```text
X-Correlation-ID: test-correlation-id
```

Verify:

- response contains `X-Correlation-ID`
- value equals `test-correlation-id`

### Generated ID

Send no correlation header.

Verify:

- response contains a non-blank `X-Correlation-ID`

Do not assert a specific UUID value.

## 10. OpenAPI Contract Fidelity

Validate the most important public contract behavior against `docs/openapi.yaml`.

At minimum verify:

- exact GET path
- exact update path
- exact HTTP methods
- success status codes
- response content type
- required response fields
- balance represented as a JSON number
- exactly the intended response shape

Do not build a large custom OpenAPI validation framework unless one already exists.

If an existing compatible library can validate the contract with minimal complexity, use it only if justified.

Otherwise, focused HTTP assertions are sufficient.

## 11. Exactly Two Business Endpoints

Add a lightweight guard against accidental API expansion if practical.

The goal is to ensure the assignment still exposes only the two required business operations.

Do not fail tests because of framework endpoints such as:

- error handling
- Swagger UI
- OpenAPI docs
- actuator endpoints if already intentionally enabled

Focus on application business controllers.

Avoid brittle reflection-heavy tests unless necessary.

## 12. Test Data Builders / Fixtures

If repeated test setup becomes noisy, create small test-only fixtures/builders.

Place them under test sources.

Examples:

```text
src/test/kotlin/.../fixture/
```

Keep them domain-specific and explicit.

Do not create production `TestUtils` or global utility packages for test convenience.

Avoid huge generic fixture frameworks.

## 13. Test Isolation

Each test must be independently executable.

Reset database state between tests using the simplest reliable method.

Possible approaches:

- repository cleanup
- transaction rollback for tests where appropriate
- explicit SQL cleanup
- a small test fixture reset helper

Be careful when using test-level transactions for HTTP E2E tests because server-side requests may execute in different transaction boundaries.

Do not make tests order-dependent.

## 14. Assertions

Prefer assertions on observable behavior.

Good:

- HTTP status
- JSON shape
- persisted database values
- withdrawal preservation
- response headers
- rollback outcome

Avoid assertions on:

- private methods
- internal implementation class names
- exact log formatting
- incidental collection implementation types

## 15. No Mocking Across the Main E2E Path

For the primary E2E tests, do not mock:

- controller
- application service
- domain calculator
- persistence adapter
- Spring Data repository
- PostgreSQL

The point is to prove that the real stack works together.

Mocks remain appropriate in focused unit tests.

## 16. Performance and Stability

Keep the E2E suite fast enough for a take-home project.

Avoid:

- sleeps
- polling without need
- random timing assumptions
- starting many containers
- excessive context restarts

Use deterministic test data.

Do not add performance/load testing unless explicitly required.

## 17. Security First

Do not put real credentials in test code.

Use ephemeral Testcontainers credentials/configuration.

Do not log secrets.

Do not disable security scanning to make tests pass.

If dependencies change, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

Avoid adding a new test library unless it materially improves the verification and cannot be achieved cleanly with the existing stack.

## 18. Documentation

Update `README.md` with a concise testing section if one does not already exist.

Document:

- unit/characterization tests
- PostgreSQL Testcontainers integration tests
- E2E/API tests
- command to run everything

Primary command:

```bash
./mvnw clean test
```

Mention that E2E tests use a real PostgreSQL container and require Docker.

Do not over-document individual test classes.

Update `docs/architecture-guidelines.md` only if useful to describe the testing strategy.

## Verification

After implementation:

1. Run:

```bash
./mvnw clean test
```

2. Confirm PostgreSQL Testcontainers starts successfully.

3. Confirm Flyway runs in integration tests.

4. Confirm empty-database E2E scenario passes.

5. Confirm GET with deposits/withdrawals passes.

6. Confirm update-all then GET passes.

7. Confirm balances are actually persisted in PostgreSQL.

8. Confirm withdrawals remain unchanged.

9. Confirm real transaction rollback/atomicity test passes.

10. Confirm supplied correlation ID is propagated.

11. Confirm generated correlation ID is returned.

12. Confirm API response matches `docs/openapi.yaml`.

13. Confirm no extra business functionality was introduced.

14. Confirm characterization tests remain unchanged and green.

15. If dependencies changed, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

## Constraints

- Use PostgreSQL Testcontainers.
- Use real Flyway migrations.
- No H2.
- No mocks in the main E2E flow.
- Preserve legacy calculator behavior.
- Preserve `TimeDeposit`.
- Preserve `TimeDepositCalculator.updateBalance` signature.
- Preserve exact database schema names.
- Preserve `docs/openapi.yaml`.
- Exactly two business REST endpoints.
- Keep test fixtures in test code.
- Avoid global `TestUtils`.
- Avoid sleeps and flaky timing assumptions.
- Keep the suite small and meaningful.
- Preserve Security First rules.

## Output

Before editing, provide:

1. current test-layer inventory
2. missing acceptance-level scenarios
3. proposed E2E test classes
4. PostgreSQL/Testcontainers reuse plan
5. database cleanup/isolation approach

After implementation, report:

1. Files changed
2. E2E/integration tests added
3. Testcontainers setup used
4. Flyway verification
5. Empty database scenario result
6. GET scenario result
7. Update-then-read scenario result
8. Withdrawal preservation result
9. Real rollback/atomicity result
10. Correlation ID results
11. Contract assertions implemented
12. Full `./mvnw clean test` result
13. Security scan result if dependencies changed
14. README/documentation updates
15. Confirmation that API contract, schema, and business behavior were unchanged

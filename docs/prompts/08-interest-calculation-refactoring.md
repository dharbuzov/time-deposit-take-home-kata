# 08 - Interest Calculation Refactoring

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- current domain/application structure
- existing `TimeDeposit`
- existing `TimeDepositCalculator`
- characterization tests
- persistence adapter and mapping tests

Inspect the current implementation before making changes.

Assume:

- characterization tests are complete
- infrastructure baseline is complete
- OpenAPI contract is aligned
- security baseline is complete
- hexagonal architecture skeleton is complete
- persistence adapter is complete

## Goal

Refactor the interest calculation into a clean and extensible design while preserving the observable behavior of the existing `TimeDepositCalculator.updateBalance` method exactly.

The assignment explicitly states that the existing calculator behavior is correct.

This step improves design, not business rules.

Do not implement REST endpoints yet.
Do not change the database schema.
Do not change the OpenAPI contract.

## 1. Protected Legacy API

Do not change the public contract of `TimeDeposit`.

Do not change the signature of:

```code
TimeDepositCalculator.updateBalance(xs: List<TimeDeposit>)
```

Existing callers must continue to work without modification.

Preserve the fact that the calculator updates the supplied deposits according to its current observable behavior.

Do not replace the method with a new incompatible API.

## 2. Characterization Tests Are the Safety Net

Before refactoring, review all characterization tests.

They must lock the existing behavior including:

- empty input
- mutation behavior
- unknown plan type behavior
- case-sensitive plan matching
- Basic boundaries
- Student boundaries
- Premium boundaries
- existing rounding behavior

Do not change characterization tests merely to make the refactoring pass.

If a characterization test conflicts with the current production behavior, investigate and report it before changing production code.

## 3. Preserve Exact Existing Rules

The current observable rules must remain unchanged.

### General

For all existing plans:

```text
days <= 30
```

means no interest.

Unknown plan types receive no interest.

Plan matching remains compatible with the existing lowercase string behavior unless the assignment explicitly requires otherwise.

Do not silently make matching case-insensitive.

### Basic

```text
days <= 30 -> no interest
days > 30  -> 1% / 12
```

### Student

```text
days <= 30       -> no interest
31 <= days <=365 -> 3% / 12
days >= 366      -> no interest
```

### Premium

```text
days <= 45 -> no interest
days > 45  -> 5% / 12
```

These boundaries must remain exactly characterized.

## 4. Preserve Existing Rounding Semantics

The legacy implementation rounds calculated interest to two decimal places using HALF_UP semantics before adding it to the balance.

Preserve the observable result exactly.

Do not perform a broad migration of the protected legacy API from `Double` to `BigDecimal`.

New internal monetary calculation code may use `BigDecimal` where it can preserve the exact characterized behavior.

Be particularly careful that changing from the legacy:

```text
Double -> BigDecimal -> round -> Double
```

behavior to mathematically cleaner decimal arithmetic may produce different observable results.

Characterization tests decide compatibility.

Do not "fix" legacy rounding unless the assignment requires it.

## 5. Extensible Interest Policy Design

Refactor the calculation so adding a future interest plan or changing plan-specific rules requires a small localized change.

Prefer a simple Strategy/Policy design.

A reasonable direction is an abstraction conceptually similar to:

```text
InterestPolicy
```

with implementations for:

- Basic
- Student
- Premium

The exact class/interface names are up to the implementation.

Keep the abstraction small.

Avoid:

- deep inheritance
- generic rule engines
- expression engines
- reflection-based strategy discovery
- annotation-driven business rules
- excessive factories
- speculative configuration systems

The assignment is small.

## 6. Policy Responsibility

A policy should own the plan-specific interest decision/calculation.

Keep orchestration separate from plan-specific rules where practical.

The design should make it obvious:

- which policy applies
- whether interest applies at the given day boundary
- how the interest amount is calculated

Avoid duplicating the common first-30-days rule unnecessarily if it can remain clear and compatible.

Do not make the design more abstract than required.

## 7. Policy Resolution

Use a simple and explicit mechanism to select the appropriate policy for a plan type.

Preserve existing behavior for unknown plan types.

Unknown values must not cause a new exception if the legacy implementation previously ignored them.

Do not silently introduce a default interest policy.

Do not make plan matching case-insensitive unless explicitly required.

## 8. Legacy Calculator as Compatibility Boundary

Keep `TimeDepositCalculator` as the compatibility entry point expected by the assignment.

It may delegate to the new policy-based implementation internally.

The public method should remain easy to understand.

The legacy class should not become responsible for:

- REST
- JPA
- database transactions
- repositories
- Spring MVC
- persistence mapping

Keep interest calculation a business concern.

## 9. Spring Independence

Interest policies and core calculation logic should not require Spring.

Do not add `@Component`, `@Service`, or other framework annotations to pure domain calculation policies merely for discovery.

Prefer plain Kotlin objects/classes and explicit composition.

If Spring wiring is eventually needed at the application boundary, keep it outside the core calculation rules.

## 10. Tests

Keep all characterization tests.

Add focused unit tests for the new policy design where they add value.

At minimum ensure coverage for:

### Basic

- day 30
- day 31
- representative value above 30

### Student

- day 30
- day 31
- day 365
- day 366

### Premium

- day 30
- day 31
- day 45
- day 46

### Other

- unknown plan
- case sensitivity
- rounding-sensitive example
- multiple deposits in one call

Avoid duplicating every characterization test unnecessarily if the compatibility suite already provides the same protection.

Prefer readable boundary tests over clever parameterization.

## 11. No Persistence Coupling

Do not make interest policies depend on JPA entities.

Do not calculate interest directly inside persistence adapters.

Persistence may map data into the model required by the calculator/application layer, but business calculation must remain independent of PostgreSQL/JPA.

Do not change the schema in this step.

## 12. No Transaction or Concurrency Expansion

This step is about calculation design.

Do not add:

- pessimistic locks
- optimistic locking/version columns
- distributed locks
- JVM synchronization
- Saga
- TCC
- 2PC

The update-all transaction boundary belongs to the application use-case step.

## 13. Security First

Follow existing Security First repository rules.

Do not add dependencies for this refactoring unless genuinely required.

A Strategy/Policy refactor should normally require no new external library.

If build/dependency configuration changes, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

## 14. Documentation

Update documentation only if the implemented design materially changes what should be explained.

### docs/architecture-guidelines.md

If useful, document briefly that plan-specific interest behavior uses an explicit Strategy/Policy approach and remains framework-independent.

### README.md

Keep business rules aligned with the actual characterized behavior.

Do not rewrite README with implementation trivia.

Do not change `docs/openapi.yaml`.

Do not change `docs/erd.puml`.

## Verification

After implementation:

1. Run:

```bash
./mvnw clean test
```

2. Confirm every existing characterization test passes unchanged.

3. Confirm Basic boundaries are unchanged.

4. Confirm Student boundaries are unchanged.

5. Confirm Premium boundaries are unchanged.

6. Confirm unknown plan behavior is unchanged.

7. Confirm case sensitivity is unchanged.

8. Confirm rounding-sensitive behavior is unchanged.

9. Confirm `TimeDeposit` public contract is unchanged.

10. Confirm `TimeDepositCalculator.updateBalance` signature is unchanged.

11. Confirm no JPA/Spring dependency leaked into interest policies.

12. Confirm database schema and OpenAPI contract were not changed.

13. If dependencies changed, run the repository security scan.

## Constraints

- Existing calculator behavior is correct.
- Refactor design, not business semantics.
- Characterization tests must remain unchanged and green.
- Preserve `TimeDeposit`.
- Preserve `TimeDepositCalculator.updateBalance` signature.
- Preserve mutation behavior.
- Preserve existing plan matching behavior.
- Preserve unknown-plan behavior.
- Preserve exact day boundaries.
- Preserve observable rounding behavior.
- Do not change persistence schema.
- Do not change `docs/openapi.yaml`.
- Do not implement REST endpoints.
- Do not add infrastructure.
- Do not introduce a rule engine.
- Prefer a small Strategy/Policy design.
- Keep core calculation framework-independent.
- Preserve Security First rules.

## Output

Before editing, provide a short refactoring plan and explicitly state the legacy invariants that will be preserved.

After implementation, report:

1. Files changed
2. Policies/strategies introduced
3. Policy resolution approach
4. How `TimeDepositCalculator` delegates to the new design
5. How legacy rounding was preserved
6. Tests added or changed
7. Full test results
8. Confirmation that characterization tests were not weakened
9. Confirmation that `TimeDeposit` was not broken
10. Confirmation that `updateBalance` signature and observable behavior were preserved
11. Confirmation that API contract and database schema were unchanged
12. Any design decisions intentionally deferred to the application use-case step

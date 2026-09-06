# 16 - Production Update-All with Monthly Idempotency and Eligibility

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- current `TimeDeposit`
- current `TimeDepositCalculator`
- current interest policy/refactoring code
- current update-all use case
- current persistence ports/adapters
- current Flyway migrations
- current datasource/Hikari configuration
- current logging/correlation-ID setup
- current unit/integration/E2E tests

Inspect the repository before editing.

## Goal

Implement a production-minded `update all time deposit balances` mechanism in the existing Hexagonal Architecture.

The implementation must be:

- batch-based
- bounded
- parallel with bounded concurrency
- transaction-per-batch
- safe to retry
- safe across repeated HTTP requests
- safe across multiple threads
- safe across multiple application instances
- idempotent per eligible time deposit per calendar month
- driven by the existing `days` business rules
- strongly tested
- clearly documented with KDoc/JavaDoc and README justification
- well logged without noisy per-record INFO logging

Do not change the protected legacy calculator behavior.

---

# 1. Existing Business Logic Is the Source of Truth

The legacy calculator is:

```kotlin
class TimeDepositCalculator {
    fun updateBalance(xs: List<TimeDeposit>) {
        for (i in xs.indices) {
            var interest = 0.0
            if (xs[i].days > 30) {
                if (xs[i].planType == "student") {
                    if (xs[i].days < 366) {
                        interest += xs[i].balance * 0.03 / 12
                    }
                } else if (xs[i].planType == "premium") {
                    if (xs[i].days > 45) {
                        interest += xs[i].balance * 0.05 / 12
                    }
                } else if (xs[i].planType == "basic") {
                    interest += xs[i].balance * 0.01 / 12
                }
            }
            val a2d = BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)
            xs[i].balance += a2d.toDouble()
        }
    }
}
```

Preserve its exact observable behavior.

Interpretation:

```text
Basic:
days <= 30 -> not eligible
days > 30  -> one monthly interest amount = balance * 0.01 / 12

Student:
days <= 30 -> not eligible
31..365    -> one monthly interest amount = balance * 0.03 / 12
366+       -> not eligible

Premium:
days <= 45 -> not eligible
46+        -> one monthly interest amount = balance * 0.05 / 12
```

The `/ 12` means one invocation applies one monthly interest amount.

Important distinction:

```text
days            -> determines eligibility
accrual period  -> determines idempotency
```

Do NOT replace eligibility with calendar period logic.

Do NOT derive idempotency from `days`.

---

# 2. Why `days` Cannot Be the Idempotency Key

Do not use:

```text
UNIQUE(time_deposit_id, days)
```

because that would permit monthly interest to be applied again every time `days` changes.

Example:

```text
days=100 -> POST -> interest applied
days=101 -> POST -> interest applied again
```

That would effectively allow daily accrual while the calculator clearly applies a monthly amount.

Therefore:

- `days` controls whether interest is eligible
- a calendar monthly accrual period prevents duplicate monthly application

---

# 3. Required High-Level Flow

Target:

```text
POST /time-deposits/balances
        |
        v
capture operation period once
        |
        v
capture upperBoundId
        |
        v
read deposit IDs in bounded batches
        |
        v
dispatch batches to bounded workers
        |
        +-------------------+
        |                   |
        v                   v
     worker TX           worker TX
        |                   |
        v                   v
load entities          load entities
        |                   |
        v                   v
check eligibility by DAYS
        |
        +---- not eligible ----------------> count notEligible
        |
        v
atomic monthly claim
        |
        +---- conflict --------------------> count alreadyProcessed
        |
        v
TimeDepositCalculator.updateBalance(...)
        |
        v
persist balance
        |
        v
COMMIT
```

If a worker batch fails:

```text
ROLLBACK
```

Rollback must revert:

- monthly claims created in that batch
- balance changes made in that batch

Previously committed batches remain committed.

---

# 4. Do Not Use `findAll()`

The update-all path must not call:

```kotlin
repository.findAll()
```

Process deposits in bounded batches.

Recommended default:

```text
batch-size = 500
```

Make it configurable.

Example:

```yaml
app:
  time-deposit:
    update:
      batch-size: 500
      workers: 4
```

Validate both values are positive.

Do not expose these tuning values through REST.

---

# 5. Stable Upper Bound

At the beginning of the update operation:

```sql
SELECT MAX(id)
```

Capture:

```text
upperBoundId
```

Then only process:

```text
id <= upperBoundId
```

This defines:

> This run processes deposits that existed when the update started. Deposits inserted later are left for the next invocation.

Do not use one long transaction to obtain snapshot semantics.

---

# 6. Keyset Batch Traversal

Prefer keyset traversal over OFFSET.

Use:

```sql
WHERE id > :lastId
  AND id <= :upperBoundId
ORDER BY id
LIMIT :batchSize
```

Coordinator should obtain IDs, not managed JPA entities.

Do not pass JPA entities between threads.

Worker receives:

- immutable deposit IDs or range
- accrual period
- correlation context if propagated

and loads entities inside its own transaction.

---

# 7. Bounded Parallelism

Process independent batches with bounded concurrency.

Recommended default:

```text
workers = 4
```

Requirements:

- fixed/bounded pool
- no `parallelStream()`
- no common ForkJoinPool
- no unbounded task submission
- no thread per deposit
- no unbounded executor queue
- worker count must remain sensible relative to Hikari pool size

Inspect the actual Hikari maximum pool size.

Choose worker count conservatively.

Example rule:

```text
workers < Hikari max pool size
```

Leave connection capacity for request handling and other DB work.

Prefer a dedicated executor/TaskExecutor owned by this use case.

---

# 8. Separate Coordinator and Transactional Worker

Avoid Spring self-invocation transaction bugs.

Use separate Spring beans.

Conceptually:

```kotlin
@Service
class UpdateAllTimeDepositBalancesService(
    private val batchProcessor: TimeDepositBalanceBatchProcessor,
    ...
)
```

and:

```kotlin
@Component
class TimeDepositBalanceBatchProcessor(
    ...
) {
    @Transactional
    fun processBatch(...): BatchResult {
        ...
    }
}
```

The coordinator:

- determines period
- determines upper bound
- reads batches
- dispatches bounded tasks
- aggregates counts
- handles failure
- returns final summary

The worker:

- starts one transaction
- loads its deposits
- evaluates eligibility
- attempts atomic claim
- calculates
- persists
- returns batch counts

---

# 9. Monthly Accrual Period

Determine the period once per HTTP request.

Use:

```kotlin
YearMonth
```

Example:

```text
2026-09
```

Do not call:

```kotlin
YearMonth.now()
```

independently inside every worker.

Use an injected `Clock` or period provider so tests are deterministic.

Persist the period in a stable form, for example:

```text
VARCHAR(7)
```

with ISO `YYYY-MM`.

The period exists only for monthly idempotency.

It must not replace `days` eligibility logic.

---

# 10. Technical Idempotency Table

Do NOT modify the existing `timeDeposits` table.

Add a Flyway migration for a technical table such as:

```sql
CREATE TABLE time_deposit_interest_accruals (
    time_deposit_id INTEGER NOT NULL,
    accrual_period VARCHAR(7) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_time_deposit_interest_accrual
        UNIQUE (time_deposit_id, accrual_period)
);
```

Use exact naming consistent with the repository conventions.

If appropriate, add a foreign key to the existing time deposit ID.

Do not add speculative:

- status
- retry_count
- version
- updated_at
- worker_id

unless a concrete implementation need appears.

The important invariant is:

```text
UNIQUE(time_deposit_id, accrual_period)
```

---

# 11. Eligibility Must Be Checked Before Claim

This ordering is critical.

Correct:

```text
load deposit
    ↓
evaluate DAYS eligibility
    ↓
if not eligible -> skip without claim
    ↓
atomic monthly claim
    ↓
calculate
    ↓
persist
```

Incorrect:

```text
claim
    ↓
calculator returns zero because not eligible
```

Why:

Example:

```text
Premium, days=45
```

If a monthly claim is created while not eligible:

```text
September 5 -> days=45 -> claim created -> no interest
September 6 -> days=46 -> now eligible -> claim conflict -> incorrectly skipped
```

Therefore:

> Ineligible deposits MUST NOT create a monthly accrual claim.

This is a required invariant.

---

# 12. Eligibility API

Do not duplicate the nested legacy business rules casually in the batch processor.

If the current interest-policy refactor already exposes eligibility, reuse it.

Otherwise introduce the smallest domain-level capability that can answer:

```kotlin
fun isEligible(timeDeposit: TimeDeposit): Boolean
```

or equivalent.

It must preserve exact legacy boundaries.

Required tests:

```text
Basic:
30 -> false
31 -> true

Student:
30 -> false
31 -> true
365 -> true
366 -> false

Premium:
45 -> false
46 -> true

Unknown plan:
false
```

Do not modify the public signature of:

```kotlin
TimeDepositCalculator.updateBalance(...)
```

---

# 13. Atomic Claim

After eligibility succeeds, perform:

```sql
INSERT INTO time_deposit_interest_accruals(
    time_deposit_id,
    accrual_period,
    created_at
)
VALUES (:id, :period, :createdAt)
ON CONFLICT (time_deposit_id, accrual_period) DO NOTHING
```

Interpret:

```text
inserted = 1 -> monthly claim acquired -> calculate and update
inserted = 0 -> already processed in this period -> skip
```

Do NOT implement:

```text
SELECT exists(...)
then
INSERT
```

because it races.

The PostgreSQL unique constraint is the source of concurrency correctness.

---

# 14. Transactional Invariant

For every eligible claimed deposit:

```text
claim
+
calculate
+
balance update
```

must be inside the same worker transaction.

Required behavior:

```text
BEGIN

claim A
update A

claim B
update B

FAIL

ROLLBACK
```

After rollback:

- claim A does not exist
- claim B does not exist
- A balance change is gone
- B balance change is gone

The failed batch is safe to retry.

---

# 15. Multiple HTTP Requests / Threads / Instances

Correctness must hold under:

- repeated same-month POST calls
- two concurrent HTTP calls
- two worker threads
- two application instances

Do not solve this with:

- `synchronized`
- `ReentrantLock`
- in-memory Set
- JVM mutex
- Redis lock
- distributed lock

Use the DB invariant:

```text
UNIQUE(time_deposit_id, accrual_period)
```

plus atomic insert.

---

# 16. Result Model

Return a meaningful summary.

Preferred response:

```json
{
  "period": "2026-09",
  "processed": 12000,
  "updated": 10500,
  "alreadyProcessed": 1000,
  "notEligible": 500
}
```

Define semantics exactly:

```text
processed
= all deposits considered by the workers

updated
= eligible deposits whose monthly claim succeeded and whose processing completed

alreadyProcessed
= eligible deposits whose monthly claim already existed for this period

notEligible
= deposits that fail the DAYS-based business eligibility rules
```

Invariant:

```text
processed = updated + alreadyProcessed + notEligible
```

Important:

If an eligible deposit successfully acquires a claim and the legacy calculator produces a rounded zero interest amount, still count it according to the existing business semantics. Do not silently invent another status unless required by tests/contract.

Update `docs/openapi.yaml` to match the response.

Keep exactly two business endpoints.

---

# 17. Failure Semantics

If any batch fails:

- fail the overall request
- do not return a misleading success summary
- failed batch rolls back fully
- previously committed batches remain committed
- a same-month retry is safe:
  - committed deposits -> `alreadyProcessed`
  - failed batch deposits -> can claim and process again

Do not implement compensating rollback across completed batches.

Document this trade-off.

---

# 18. Logging

Add production-useful logs.

## Operation start, INFO

Example:

```text
operation=update_balances
period=2026-09
batchSize=500
workers=4
upperBoundId=12000
```

## Operation success, INFO

```text
operation=update_balances
period=2026-09
processed=12000
updated=10500
alreadyProcessed=1000
notEligible=500
durationMs=...
status=success
```

## Operation failure, ERROR

```text
operation=update_balances
period=2026-09
processed=...
updated=...
alreadyProcessed=...
notEligible=...
failedBatch=...
durationMs=...
status=failed
errorType=...
```

## Batch-level normal diagnostics, DEBUG

```text
operation=update_balance_batch
batch=...
size=500
updated=...
alreadyProcessed=...
notEligible=...
durationMs=...
```

Do not log every deposit.

Do not log:

- balance values
- withdrawals
- full entities
- credentials
- SQL parameters
- secrets

Preserve correlation ID behavior.

If MDC must cross worker threads, explicitly propagate only required context and clear it in `finally` to avoid thread-pool leakage.

---

# 19. KDoc / JavaDoc

This feature MUST be well documented in code.

Add meaningful KDoc/JavaDoc to non-obvious types/methods.

## Coordinator docs must explain

- why processing is batched
- why keyset traversal is used
- why concurrency is bounded
- why upperBoundId is captured
- why JPA entities are not passed to workers
- why one period is captured once per operation

## Batch processor docs must explain

- why transaction is per batch
- why it is a separate Spring bean
- why Spring proxy invocation matters
- claim + calculate + update atomicity
- why rollback makes failed batch retryable

## Eligibility docs must explain

- `days` is the business eligibility input
- eligibility is checked before claim
- premium day 45/46 example
- student day 365/366 boundary

## Claim adapter docs must explain

- `ON CONFLICT DO NOTHING`
- DB unique constraint is the concurrency invariant
- why check-then-insert is unsafe
- why this works across application instances

## Result DTO docs must define

- processed
- updated
- alreadyProcessed
- notEligible

## Configuration docs must explain

- batch-size trade-off
- worker count
- relationship to Hikari pool size

Do not write comments that merely restate the code.

Document invariants and reasoning.

---

# 20. Tests - Strong Coverage Required

Use unit tests where appropriate.

Use PostgreSQL/Testcontainers for:

- transaction behavior
- unique constraints
- atomic claim
- concurrency
- retry correctness

Avoid H2.

Do not weaken existing characterization tests.

## A. Eligibility boundaries

Explicitly test:

```text
Basic:
30 -> notEligible
31 -> eligible

Student:
30 -> notEligible
31 -> eligible
365 -> eligible
366 -> notEligible

Premium:
45 -> notEligible
46 -> eligible

Unknown plan:
notEligible
```

## B. Claim only eligible deposits

Critical tests:

```text
Premium days=45
POST
-> notEligible
-> NO accrual row created
```

Then change to:

```text
Premium days=46
POST same month
-> claim succeeds
-> updated
```

Also test:

```text
Basic 30 -> 31
Student 30 -> 31
```

where applicable.

## C. Same-month idempotency

First call:

```text
updated > 0
alreadyProcessed = 0
```

Second call same month:

```text
updated = 0 for previously processed eligible deposits
alreadyProcessed = previous updated count
```

Balances must not receive monthly interest twice.

## D. New month

Using deterministic clock:

```text
2026-09 -> apply
2026-09 -> skip already processed
2026-10 -> eligible deposit can apply again
```

Eligibility still depends on current `days`.

## E. Empty DB

Verify:

```text
processed = 0
updated = 0
alreadyProcessed = 0
notEligible = 0
```

## F. Multiple batches

Use small test configuration:

```text
batch-size = 2
workers = 2
```

Seed 5+ deposits.

Verify:

- all are considered
- no duplicates
- partial final batch works
- result invariant holds

## G. Upper-bound semantics

1. capture operation upper bound
2. insert a newer deposit
3. continue processing

Verify the new deposit is not part of that run.

## H. Atomic claim concurrency

Use real PostgreSQL.

Two concurrent workers attempt same:

```text
time_deposit_id
+
accrual_period
```

Verify:

```text
exactly one claim succeeds
```

and balance is updated at most once.

## I. Concurrent HTTP calls

If practical, run two update-all HTTP requests concurrently against same dataset/period.

Verify:

- no deposit receives interest twice
- DB accrual table has at most one row per deposit/period
- aggregate final balances are correct

## J. Batch rollback

Force failure inside one worker transaction after one or more successful claims/updates.

Verify:

- failed batch claims are rolled back
- failed batch balance changes are rolled back
- previous committed batches remain committed

## K. Retry after partial failure

Scenario:

```text
batch 1 -> committed
batch 2 -> fails and rolls back
request -> fails
retry same month
```

Verify:

```text
batch 1 eligible deposits -> alreadyProcessed
batch 2 eligible deposits -> updated
```

No monthly double-interest.

## L. DB uniqueness

Verify actual PostgreSQL constraint:

```text
UNIQUE(time_deposit_id, accrual_period)
```

Duplicate claim must not create another row.

## M. Result counts

For successful request:

```text
processed = updated + alreadyProcessed + notEligible
```

Verify exact `period` format.

## N. Logging

Avoid brittle full-string assertions.

Only test meaningful helpers/behavior if introduced:

- correlation ID propagation
- MDC cleanup
- no worker failure caused by logging code

Do not test every log line.

---

# 21. Deterministic Time

Do not use real current date/month directly in tests.

Inject:

```kotlin
Clock
```

or a small period provider.

Production:

```text
system clock
```

Tests:

```text
fixed clock
```

All workers in one update operation must use the same captured period.

---

# 22. README Justification

Add a clear section to `README.md`, for example:

```text
Bulk Balance Update Design
```

Include the following reasoning.

## Why not findAll()

> Updating every time deposit through an unbounded `findAll()` makes memory consumption and transaction size grow with the complete table. The implementation therefore traverses deposits in bounded batches.

## Why bounded workers

> Batches can be processed independently, so a small bounded worker pool improves throughput while keeping database pressure controlled. Worker count is deliberately constrained relative to the database connection pool.

## Why one transaction per batch

> One transaction across the complete dataset would create a long-running transaction with large rollback scope and prolonged resource retention. Each worker batch therefore executes in its own transaction.

## `days` versus accrual period

> `days` is part of the supplied business rules and determines whether a deposit is currently eligible for interest. The calculator applies a monthly interest amount (`annualRate / 12`), therefore idempotency is tracked separately by calendar accrual period. `days` is not used as the idempotency key.

## Why eligibility comes before claim

> An ineligible deposit must not consume its monthly accrual slot. For example, a Premium deposit at day 45 is not eligible, but at day 46 it becomes eligible. Therefore the system checks `days` eligibility first and only then attempts the monthly claim.

## Why technical accrual table

> Monthly processing metadata is infrastructure state rather than part of the protected `TimeDeposit` model. It is stored in a dedicated technical table and does not modify the assignment's existing `timeDeposits` schema.

## Why unique constraint

> `UNIQUE(time_deposit_id, accrual_period)` is the atomic idempotency and concurrency invariant. It prevents duplicate monthly application across repeated HTTP requests, concurrent worker threads, and multiple application instances.

## Why claim and balance update share a transaction

> The claim and balance change are part of the same batch transaction. If the batch fails, both are rolled back, allowing the failed work to be safely retried.

## Partial failure semantics

> The operation is not globally atomic across the entire table. Successfully committed batches remain committed if a later batch fails. A retry is safe because already committed eligible deposits are rejected by the monthly unique claim while the failed batch's rolled-back claims are available again.

## Why no distributed lock

> Database uniqueness already provides the required cross-thread and cross-instance correctness, so JVM or distributed locks would add complexity without improving the invariant.

Keep these as explicit design decisions / assumptions.

---

# 23. Architecture Guidelines

Update `docs/architecture-guidelines.md` with concise rules:

- update-all must use bounded batches
- use keyset traversal
- bounded worker concurrency
- transaction per batch
- worker owns transaction
- no JPA entities across threads
- `days` controls eligibility
- eligibility before claim
- calendar period controls idempotency
- atomic DB claim
- unique constraint is concurrency authority
- claim and balance update share transaction
- safe retry after rollback
- no JVM/distributed locks for this invariant
- partial failure semantics are explicit

---

# 24. AGENTS.md

Add future implementation rules:

- never use unconditional `findAll()` for update-all
- process bounded batches
- use bounded workers only
- never use `parallelStream()` for this flow
- worker transaction must be opened through a separate Spring bean
- never pass JPA entities between workers
- evaluate `days` eligibility before attempting monthly claim
- ineligible deposits must not create accrual rows
- never implement check-then-insert for claims
- preserve `UNIQUE(time_deposit_id, accrual_period)`
- claim + balance update must be atomic in one batch transaction
- no distributed/JVM lock for this invariant
- use deterministic time in tests
- concurrency/rollback semantics require PostgreSQL/Testcontainers coverage
- comments should explain invariants and trade-offs

---

# 25. Security First

Do not log balance values or withdrawal data.

Do not expose accrual-table internals through REST.

Do not add new infrastructure.

Prefer existing Java/Spring concurrency primitives.

If dependencies/build configuration change:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

---

# 26. Verification

After implementation:

1. Run:

```bash
./mvnw clean test
```

2. Confirm update-all has no unconditional `findAll()`.

3. Confirm keyset ID traversal.

4. Confirm batch size is bounded/configurable.

5. Confirm worker count is bounded/configurable.

6. Confirm worker count is sensible relative to Hikari max pool size.

7. Confirm no JPA entity crosses worker threads.

8. Confirm one transaction per worker batch.

9. Confirm transactional worker is invoked through a separate Spring bean/proxy.

10. Confirm one `YearMonth`/period is captured per operation.

11. Confirm `days` determines eligibility.

12. Confirm eligibility is evaluated BEFORE claim.

13. Confirm ineligible deposits create NO accrual row.

14. Confirm Premium day 45 -> 46 works in the same calendar month.

15. Confirm Student day 365 -> eligible and 366 -> not eligible.

16. Confirm Basic day 30 -> 31 transition works.

17. Confirm claim uses PostgreSQL `ON CONFLICT DO NOTHING`.

18. Confirm DB has:

```text
UNIQUE(time_deposit_id, accrual_period)
```

19. Confirm claim + calculation + update are in the same transaction.

20. Confirm same-month repeat cannot apply interest twice.

21. Confirm next-month eligible processing can happen again.

22. Confirm concurrent workers cannot double-apply interest.

23. Confirm concurrent HTTP requests cannot double-apply interest.

24. Confirm failed batch rolls back claims and balances.

25. Confirm retry after partial failure is safe.

26. Confirm prior committed batches remain committed.

27. Confirm response invariant:

```text
processed = updated + alreadyProcessed + notEligible
```

28. Confirm logs contain operation summary and no sensitive balance data.

29. Confirm KDoc/JavaDoc explains concurrency, idempotency, eligibility, and transaction invariants.

30. Confirm README contains the full design justification.

31. Confirm `docs/openapi.yaml` matches the response.

32. Confirm exactly two business endpoints remain.

33. Confirm legacy calculator characterization tests remain green.

34. If dependencies changed, run dependency/security checks.

---

# Constraints

- Preserve `TimeDepositCalculator.updateBalance` signature.
- Preserve exact legacy calculator behavior.
- Do not change existing `timeDeposits` schema.
- Add a separate technical accrual/idempotency table.
- `days` determines eligibility.
- Calendar month determines idempotency.
- Eligibility MUST happen before claim.
- Ineligible deposits MUST NOT create claims.
- No `findAll()` for update-all.
- Bounded batches.
- Bounded workers.
- Transaction per batch.
- Separate Spring transactional worker bean.
- No JPA entities between threads.
- Atomic PostgreSQL claim.
- Unique `(time_deposit_id, accrual_period)` invariant.
- Claim + calculate + update in same transaction.
- Safe retry after batch failure.
- No global transaction across all deposits.
- No JVM/distributed lock.
- No `parallelStream()`.
- Strong Testcontainers concurrency and rollback tests.
- Meaningful KDoc/JavaDoc.
- Production-useful logging.
- Explicit README justification.
- Preserve Security First rules.

---

# Output

Before editing, report:

1. Current update-all implementation
2. Current transaction boundary
3. Current repository access pattern
4. Any `findAll()` usage
5. Current Hikari maximum pool size
6. Proposed batch size
7. Proposed worker count
8. Proposed bounded executor strategy
9. Proposed upper-bound/keyset traversal
10. Proposed eligibility API
11. Proposed accrual-table migration
12. Proposed period representation
13. Proposed atomic claim implementation
14. Proposed result DTO
15. Existing tests that conflict with the new semantics
16. Proposed README justification

After implementation, report:

1. Files changed
2. Flyway migration
3. Eligibility implementation
4. Application port changes
5. Coordinator implementation
6. Transactional batch processor
7. Transaction ownership
8. Executor configuration
9. Hikari/concurrency reasoning
10. Upper-bound/keyset traversal
11. Claim adapter implementation
12. Unique constraint
13. Period/Clock implementation
14. Proof eligibility occurs before claim
15. Calculation integration
16. Response DTO/OpenAPI changes
17. Logging changes
18. KDoc/JavaDoc added
19. Unit tests added
20. PostgreSQL integration tests added
21. Eligibility-boundary tests
22. Concurrency tests
23. Rollback/retry tests
24. Same-month idempotency test
25. New-month test
26. Premium 45 -> 46 same-month test
27. Full `./mvnw clean test` result
28. Security scan result if dependencies changed
29. README justification added
30. AGENTS/architecture documentation updates
31. Confirmation that legacy calculator behavior and existing `timeDeposits` schema remain unchanged

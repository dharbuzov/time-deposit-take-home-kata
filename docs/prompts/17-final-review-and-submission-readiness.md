# 17 - Final Review and Submission Readiness

Act as the senior engineer evaluating this take-home before submission.

This is a review, verification, cleanup, testing, and documentation task. Do not add new features unless required to fix correctness, assignment compliance, security, or a clear quality defect.

## Read first

Inspect the complete repository before editing:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- `docs/erd.puml`
- `docs/prompts/`
- `kotlin/pom.xml`
- Maven Wrapper
- application configuration
- Flyway migrations
- Dockerfile and `compose.yaml`
- domain/application/adapters
- GET pagination
- update-all batching/concurrency/idempotency
- logging/correlation ID
- demo seeding
- all tests

Documentation is not automatically authoritative. Compare it with actual code and the assignment.

## 1. Evaluator pass

Classify findings:

- BLOCKER: build/test failure, missing required behavior, broken schema/API, changed legacy behavior, concurrency correctness bug
- MAJOR: unsafe `findAll()` update, wrong transaction boundary, claim-before-eligibility, unbounded concurrency, JPA entities across threads, OpenAPI/code mismatch, serious N+1
- MINOR: naming, stale comments, missing useful KDoc, unused dependencies
- NICE-TO-HAVE: cosmetic/optional infrastructure

Fix BLOCKER and MAJOR. Fix low-risk MINOR issues where clearly beneficial. Do not spend time on NICE-TO-HAVE architecture.

## 2. Assignment compliance

Verify exactly two business endpoints exist:

```text
GET /time-deposits
POST /time-deposits/balances
```

No accidental CRUD or withdrawal endpoints.

GET must return deposits including:

- id
- planType
- balance
- days
- withdrawals

Verify intentional pagination is implemented/documented consistently using `page`, `size`, `sort`, bounded size, explicit response DTO/metadata, safe sort whitelist, and no direct Spring `Page` exposure.

Verify README explicitly justifies pagination as a scalability interpretation of the literal "retrieve all" requirement.

## 3. Legacy calculator behavior

Do not change `TimeDepositCalculator.updateBalance` signature or protected `TimeDeposit` contract.

Verify characterization tests cover exact observable behavior:

```text
Basic:   <=30 no interest, 31+ -> 1% / 12
Student: <=30 no interest, 31..365 -> 3% / 12, 366+ no interest
Premium: <=45 no interest, 46+ -> 5% / 12
Unknown plan -> no interest
```

Also preserve case sensitivity, rounding and mutation semantics.

## 4. Eligibility versus idempotency

Verify the design clearly separates:

```text
days -> business eligibility
YearMonth/accrual period -> monthly idempotency
```

Critical invariant:

```text
eligibility BEFORE claim
```

Verify:

```text
Basic 30 -> no claim, 31 -> eligible
Premium 45 -> no claim, 46 -> eligible
Student 365 -> eligible, 366 -> not eligible
```

A Premium deposit processed at day 45 must create NO accrual claim so it can become eligible at day 46 in the same month.

Treat violation as BLOCKER.

## 5. Update-all bulk processing

Verify production flow resembles:

```text
POST
 |
capture period + upperBoundId
 |
keyset ID batches
 |
bounded worker pool
 /   |   \
TX   TX   TX
 |
load entities inside worker
 |
check days eligibility
 |
atomic monthly claim
 |
legacy calculation
 |
persist
 |
commit
```

Search specifically for unsafe patterns:

```text
findAll()
parallelStream()
Executors.newCachedThreadPool()
new Thread(...)
unbounded CompletableFuture submission
JPA entity passed to worker
@Transactional self-invocation
SELECT/check then INSERT claim
```

Fix unsafe occurrences on the update-all path.

Verify:

- configurable bounded batch size
- keyset/ID traversal rather than full-table materialization
- stable upper bound
- bounded workers
- worker count sensible relative to Hikari max pool
- no JPA entities cross threads
- one transaction per batch
- transactional processor is a separate Spring bean invoked through proxy
- no global transaction across the entire update
- transaction does not span waiting for unrelated worker futures

## 6. PostgreSQL idempotency

Verify Flyway creates the technical accrual table and actual PostgreSQL invariant:

```text
UNIQUE(time_deposit_id, accrual_period)
```

Verify claim is atomic:

```text
INSERT ...
ON CONFLICT (time_deposit_id, accrual_period) DO NOTHING
```

Do not use check-then-insert.

Claim + calculation + balance update must share the same worker transaction.

Correctness must hold across:

- repeated same-month HTTP calls
- concurrent workers
- concurrent HTTP calls
- multiple application instances

Do not use JVM or distributed locks for this invariant.

The technical table must not leak into domain/API models.

## 7. Failure and retry semantics

Verify a failed batch rolls back both:

- claims created in that batch
- balance changes from that batch

Previously committed batches remain committed.

A same-month retry must:

- skip already committed deposits
- process deposits whose failed claims rolled back
- never apply monthly interest twice

README must explain that update-all is not globally atomic across the complete table.

## 8. Response contract

Verify update-all returns/documented summary consistently, preferably:

```json
{
  "period": "2026-09",
  "processed": 12000,
  "updated": 10500,
  "alreadyProcessed": 1000,
  "notEligible": 500
}
```

Verify invariant:

```text
processed = updated + alreadyProcessed + notEligible
```

Check implementation, DTOs, tests and `docs/openapi.yaml` all agree.

Remove stale 204 documentation if the endpoint now returns a body.

## 9. Required database schema

Assignment schema remains authoritative.

Verify exact prescribed names remain:

```text
timeDeposits
  id
  planType
  days
  balance

withdrawals
  id
  timeDepositId
  amount
  date
```

Do not normalize these required names to snake_case.

The technical accrual table may use its deliberate technical naming convention.

Synchronize `docs/erd.puml` with actual Flyway/JPA schema.

## 10. Money

New monetary persistence/API code should use `BigDecimal`.

Legacy `TimeDeposit.balance: Double` remains because it is protected.

Keep BigDecimal <-> legacy Double conversion explicit and isolated.

OpenAPI money is JSON number without `format: double`.

## 11. Hexagonal Architecture

Verify:

```text
REST Adapter
     ↓
Application / Use Cases
     ↓
Domain
     ↑
Outbound Ports
     ↑
Persistence Adapter
     ↓
PostgreSQL
```

Check:

- domain has no Spring/JPA/REST
- application does not depend on Spring Data repositories
- ports do not expose Page/Pageable/JPA entities/EntityManager
- controllers are thin
- REST mapping belongs to REST adapter
- persistence mapping belongs to persistence adapter
- eligibility/calculation belongs to domain
- transaction/concurrency orchestration stays outside domain
- no garbage-drawer global `utils`

Do not refactor merely to satisfy aesthetics.

## 12. GET pagination

Verify OpenAPI, implementation, README and tests agree on:

- `page`
- `size`
- `sort`
- zero-based page semantics if documented
- defaults
- maximum size
- allowed sort fields
- pagination response metadata
- invalid parameter behavior
- withdrawals

Inspect withdrawal loading for obvious N+1 behavior.

README must explicitly explain why unbounded GET was intentionally replaced by bounded pagination.

## 13. Hikari/concurrency

Inspect actual Hikari max pool size and actual update worker count.

Document the reasoning, e.g.:

```text
Hikari max = 10
workers = 4
```

leaves connections for request/coordinator/other DB activity.

Do not blindly increase pool sizes.

## 14. Logging

Review logging for useful operation-level observability.

Successful update should make it possible to determine:

```text
operation
period
processed
updated
alreadyProcessed
notEligible
durationMs
status
```

Normal batch diagnostics should be DEBUG, not noisy INFO-per-record logging.

Never log:

- balances
- withdrawals
- credentials
- secrets
- full entities/request bodies
- SQL parameters containing business data

Verify correlation ID remains correct.

If MDC is propagated to worker threads, copy only required context and restore/clear in `finally` to prevent thread-pool leakage.

Avoid duplicate catch-log-rethrow logging.

## 15. KDoc / JavaDoc

Review non-trivial code and add concise meaningful documentation where missing.

Especially document:

- update coordinator
- transactional batch processor
- eligibility abstraction
- atomic claim adapter
- result DTO semantics
- pagination models where non-obvious
- executor/batch configuration

Comments should explain WHY, invariants and trade-offs, especially:

- eligibility before claim
- transaction per batch
- DB unique constraint
- safe retry
- bounded concurrency
- no JPA entities across threads
- partial failure semantics

Remove stale/misleading comments. Do not comment obvious syntax.

## 16. Test coverage

Do not judge only by percentage. Verify tests prove important invariants.

Required areas:

### Legacy characterization
- empty
- mutation
- case-sensitive plan matching
- unknown plan
- rounding
- Basic 30/31
- Student 30/31/365/366
- Premium 45/46

### GET
- defaults
- explicit page/size
- sorting
- metadata
- empty page
- invalid page/size
- maximum size
- withdrawals

### PostgreSQL/Testcontainers
- Flyway
- required schema
- pagination behavior
- unique accrual constraint
- atomic claim

### Update-all
- empty DB
- one/many deposits
- multiple batches
- partial final batch
- upper-bound behavior
- result count invariant

### Eligibility
- Basic 30 -> 31
- Premium 45 -> 46
- Student 365 -> 366
- ineligible deposit creates no claim

### Idempotency
- first same-month run applies
- second same-month run skips
- next month can apply again if eligible

### Concurrency
- concurrent atomic claim
- concurrent workers
- concurrent HTTP requests if practical
- no double interest

### Rollback/retry
- failed batch rolls back claim
- failed batch rolls back balance
- previous batch remains committed
- retry skips committed work
- retry processes rolled-back work

Use real PostgreSQL for DB/transaction/concurrency invariants.

## 17. Test quality

Remove/fix:

- flaky sleeps
- order-dependent tests
- use of real current month
- dirty shared DB state
- H2 for PostgreSQL-specific behavior
- mocks that hide DB concurrency semantics

Prefer fixed `Clock`, Testcontainers, latches/barriers/futures, deterministic fixtures.

Do not weaken tests to make build green.

## 18. Seeder

Verify demo seed is:

- disabled by default
- configurable
- deterministic
- idempotent
- non-destructive
- synthetic
- free of secrets/PII

Useful seed boundaries:

```text
Basic 30 / 31
Student 365 / 366
Premium 45 / 46
```

Do not put optional demo data into a production Flyway data migration.

## 19. OpenAPI and Swagger

Treat `docs/openapi.yaml` as source of truth.

Verify it matches actual:

- exactly two business endpoints
- GET pagination
- GET response
- withdrawals
- update summary
- period
- processed/updated/alreadyProcessed/notEligible
- status codes
- money representation

Run the application and verify the actual Swagger URL. Do not guess it.

README should let reviewer start app, open Swagger, GET, POST update, GET again.

## 20. Docker/runtime

From a clean build state verify the documented flow, preferably:

```bash
./mvnw clean test
docker compose up --build
```

Then verify:

- PostgreSQL starts
- app starts
- Flyway succeeds
- datasource connects
- demo seed behaves as documented
- Swagger reachable
- GET works
- update works
- second same-period update demonstrates idempotency

Do not claim runtime verification unless commands actually succeeded.

## 21. Maven / Java

Preserve actual baseline. Do not accidentally require Java 21 if project remains Java 17.

Verify Maven Wrapper works.

Review unused/duplicate dependencies, stale plugins, and unnecessary explicit versions already managed by Spring Boot BOM.

Do not upgrade dependencies for aesthetics.

## 22. Security

Run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

Review High/Critical findings.

Prefer:

1. BOM-compatible upgrade
2. direct dependency upgrade
3. narrow transitive override if necessary
4. suppression only for documented false-positive/non-applicable finding

Do not silently suppress findings.

Also inspect for secrets, tokens, private keys, production credentials, sensitive logging, and unsafe arbitrary sorting.

If Dependency-Check cannot finish due to NVD/network/rate limits, report that explicitly rather than claiming success.

## 23. Code hygiene

Search for:

```text
TODO
FIXME
HACK
println(
System.out
printStackTrace
```

Review each occurrence.

Remove dead code, abandoned experiments, unused imports/dependencies, stale comments, debug output and obsolete assumptions.

Do not delete intentional limitations. Move important ones into README/design documentation.

## 24. README final pass

README must be sufficient for a reviewer with no prior context.

Verify sections cover:

- overview
- prerequisites with actual Java version
- Maven Wrapper build/test
- local run
- Docker
- verified Swagger URL
- exactly two business endpoints
- GET pagination parameters/defaults/max size
- pagination scalability justification
- Hexagonal Architecture
- database schema
- demo seeding
- bulk balance update
- bounded batches/workers
- keyset traversal and upper bound
- transaction per batch
- `days` eligibility
- monthly idempotency
- eligibility before claim
- DB unique constraint
- retry/partial-failure semantics
- logging/correlation ID
- testing/Testcontainers
- security
- assumptions/design decisions

Ensure README contains no stale Java 21, `.yml`, 204, global-transaction, old pagination, claim-before-eligibility, or `skipped` wording.

## 25. AI-assisted development

The assignment values reproducible AI usage.

README should concisely explain:

- OpenAI Codex used as implementation/review assistant
- `AGENTS.md` contains persistent repository constraints
- `docs/prompts/` contains reproducible task prompts
- architecture/OpenAPI/ERD constrain implementation
- which work was AI-assisted
- human verification process
- generated changes were validated by characterization/unit/PostgreSQL/Testcontainers/API/concurrency/rollback/build/security checks

Do not claim work/checks that were not actually performed.

Ensure prompt files in `docs/prompts/` reasonably match documented workflow.

## 26. Documentation consistency

Cross-check:

```text
README
AGENTS.md
docs/architecture-guidelines.md
docs/openapi.yaml
docs/erd.puml
Flyway
JPA mappings
application ports
REST DTOs
tests
```

Pay particular attention to stale iteration artifacts:

- Java 21 vs Java 17
- `openapi.yml` vs `docs/openapi.yaml`
- snake_case vs required camelCase assignment schema
- old update 204 vs summary body
- old global transaction assumption
- old `findAll()` approach
- old unpaginated GET
- claim-before-eligibility
- `skipped` vs `alreadyProcessed`/`notEligible`

Fix inconsistencies.

## 27. Git/repository hygiene

Inspect:

```bash
git status
git diff
git log --oneline
```

Verify `.gitignore`.

Do not commit build output, IDE noise, local DB files, Dependency-Check databases, logs, secrets, or temporary artifacts.

Atomic meaningful commits are preferred, but DO NOT rewrite/squash/fabricate/force-push history automatically. Report commit-history recommendations only.

Do not invent a public GitHub URL.

## 28. No overengineering

Confirm project has not unnecessarily introduced:

- Kafka/RabbitMQ
- Redis
- distributed locks
- Saga/TCC/2PC
- CQRS/event sourcing
- microservices
- WebFlux/reactive stack
- custom scheduler/job platform
- Kubernetes
- tracing/metrics infrastructure
- generic repository framework
- rule engine
- giant base classes

The result should look production-aware, not production-cosplay.

## 29. Final smoke test

Perform the closest practical reviewer flow:

```text
./mvnw clean test
        ↓
docker compose up --build
        ↓
Flyway + optional seed
        ↓
Swagger
        ↓
GET deposits
        ↓
POST update balances
        ↓
GET again
        ↓
POST update again same period
        ↓
verify alreadyProcessed behavior
```

Record actual results only.

## 30. Final report

After review/fixes return:

### Evaluator findings

```text
Severity | Finding | Resolution
```

Include every BLOCKER/MAJOR discovered.

### Assignment compliance

```text
Requirement | Status | Evidence
```

Status must be one of:

```text
PASS
PASS WITH DOCUMENTED ASSUMPTION
FAIL
```

### Architecture summary

Briefly describe:

- Hexagonal Architecture
- GET pagination
- bulk update
- transaction model
- bounded concurrency
- eligibility model
- idempotency model

### Test results

Report actual tests run/passed/failed/skipped where available.

### Security results

Report actual Dependency-Check result.

### Runtime results

Report actual Docker/Swagger/API smoke-test result.

### Known assumptions/trade-offs

Include:

- pagination interpretation of "retrieve all"
- `days` eligibility
- monthly accrual period
- partial batch failure
- worker/Hikari relationship

### Submission checklist

Return evidence-based:

```text
[ ] / [x] build passes
[ ] / [x] tests pass
[ ] / [x] Docker works
[ ] / [x] Swagger verified
[ ] / [x] exactly two business endpoints
[ ] / [x] OpenAPI synchronized
[ ] / [x] ERD synchronized
[ ] / [x] README complete
[ ] / [x] AI workflow documented
[ ] / [x] security scan reviewed
[ ] / [x] no secrets found
[ ] / [x] git working tree understood
```

Never mark complete without evidence.

## Final constraints

- No new business features.
- Preserve legacy calculator behavior.
- Preserve protected `TimeDeposit` contract.
- Preserve assignment-prescribed DB table/column names.
- Exactly two business endpoints.
- Keep pagination justification explicit.
- Eligibility before claim.
- Ineligible deposits create no monthly claim.
- DB-backed monthly idempotency.
- Bounded batches and workers.
- Transaction per batch.
- No JPA entities across worker threads.
- No distributed/JVM lock for the accrual invariant.
- Do not hide partial-failure semantics.
- Do not weaken tests.
- Do not silently suppress security findings.
- Do not claim verification that was not executed.
- Prefer deleting unnecessary complexity over adding architecture.
- Preserve Security First rules.

The final repository should be understandable, reproducible, testable, and defensible in a senior engineering review.

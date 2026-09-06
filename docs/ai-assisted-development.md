# AI-Assisted Development

## Approach

OpenAI Codex was used as the engineering and coding agent for this take-home. The repository itself was treated as the
AI engineering harness: durable constraints live in version-controlled files, focused prompts describe the current task,
and tests plus contracts verify the result.

```text
Repository knowledge / constraints
              ↓
        Focused task prompt
              ↓
             Codex
              ↓
       Proposed change
              ↓
   Tests + runtime verification
              ↓
         Reviewer pass
```

The operating principle was:

```text
AI proposes
repository constraints guide
tests/contracts verify
engineer owns the result
```

Codex output was not treated as automatically correct. Generated changes were reviewed against the assignment,
architecture guide, OpenAPI contract, ERD, and test results before being accepted.

## Repository as the Harness

Project constraints were kept in repository files instead of being repeated in every prompt.

```text
AGENTS.md
-> persistent rules and navigation

docs/architecture-guidelines.md
-> architecture and engineering decisions

docs/openapi.yaml
-> public API contract

docs/erd.puml
-> persistence model

tests
-> executable behavioral constraints

docs/prompts/
-> reproducible focused task workflow/history
```

`AGENTS.md` defines the persistent working rules for the agent. The most important constraints are that the shared
`TimeDeposit` class must not receive breaking changes, `TimeDepositCalculator.updateBalance(xs: List<TimeDeposit>)`
must keep its contract and observable behavior, exactly two business REST endpoints may be exposed, and unrelated
functionality should stay out of scope.

The same file also captures the engineering standards used during AI-assisted work: lightweight Hexagonal Architecture,
domain isolation from Spring/HTTP/JPA/database details, explicit application-level transaction boundaries, `BigDecimal`
for persisted monetary values, bounded pagination at REST boundaries, PostgreSQL-backed integration tests, security
verification expectations, and logging with `X-Correlation-ID`.

`docs/architecture-guidelines.md` gives the architectural shape and trade-offs. `docs/openapi.yaml` is the source of
truth for the public HTTP API. `docs/erd.puml` documents the database model, including the monthly accrual uniqueness
constraint. Tests convert the expected behavior into executable checks.

## Prompt Workflow

The prompts under `docs/prompts/` record focused phases of the work rather than one large instruction block.

1. Understand and protect

   `01-onboarding.md` and `02-characterization-tests.md` establish repository context and protect the legacy calculator
   behavior before larger changes.

2. Establish boundaries

   `03-infrastructure-baseline.md`, `04-openapi-contract-alignment.md`,
   `06-hexagonal-architecture-skeleton.md`, and `07-persistence-adapter-and-database-mapping.md` establish the build,
   API contract, architecture boundaries, and PostgreSQL/Flyway persistence adapter.

3. Implement behavior

   `08-interest-calculation-refactoring.md`, `09-application-use-cases-and-transaction-boundary.md`, and
   `10-rest-adapters-and-api-implementation.md` move business behavior into domain policies, application use cases, and
   thin REST adapters.

4. Harden

   `05-security-baseline.md`, `11-logging-and-observability-baseline.md`,
   `13-end-to-end-contract-and-integration-testing.md`, `14-demo-data-seeding.md`, `15-get-standard-pagination.md`, and
   `16-production-update-all-with-eligibility-and-monthly-idempotency.md` cover operational and correctness concerns:
   dependency hygiene, correlation IDs, E2E coverage, demo data, bounded reads, bounded update processing, idempotency,
   concurrency, and rollback semantics.

5. Review

   `17-final-review-and-submission-readiness.md`, `18-simplify-readme.md`, and `19-document-ai-harness.md` support the
   final reviewer pass and submission documentation.

This layout makes the AI workflow reproducible. Another engineer can inspect the same constraints, choose the focused
prompt for the next task, apply a scoped change, and verify it.

## Development Loop

The practical loop for non-trivial changes was:

```text
Analyze
  ↓
Identify invariants / ambiguities
  ↓
Protect existing behavior with tests
  ↓
Make the smallest design change
  ↓
Implement
  ↓
Run focused tests
  ↓
Run integration/runtime checks
  ↓
Review against repository constraints
```

Ambiguities were surfaced as design assumptions instead of being hidden in code. One important example is the separation
between eligibility and idempotency:

```text
days -> eligibility
accrual period -> idempotency
```

That interpretation keeps the legacy interest rules based on `days`, while using a monthly accrual period and database
uniqueness to prevent duplicate processing.

## Verification Guardrails

The repository contains several guardrails that constrain AI-generated changes:

- Legacy characterization tests for `TimeDepositCalculator.updateBalance`.
- Interest policy unit tests for plan-specific eligibility and calculation boundaries.
- `docs/openapi.yaml` as the REST contract for the two public business endpoints.
- Hexagonal boundaries separating REST adapters, application use cases, domain logic, outbound ports, and persistence.
- PostgreSQL and Flyway migrations for the database schema.
- PostgreSQL/Testcontainers integration tests for persistence and application behavior.
- REST and end-to-end tests for HTTP-to-database behavior.
- Concurrency and idempotency tests for monthly accrual claims.
- Rollback and retry tests for batch update failure behavior.
- Maven build execution through the project wrapper.
- Docker Compose smoke path for running the app with PostgreSQL.
- OWASP Dependency-Check configured through Maven.

The full Docker-backed Maven test suite was run successfully during final verification:

```bash
cd kotlin
./mvnw clean test
```

The command completed with all 69 tests passing and no skipped tests when Docker Desktop was available. Dependency-Check
is configured and documented as a security verification command, but this document does not claim a successful
Dependency-Check run unless that command is executed separately.

## Human Review

AI-generated code was treated as a proposed patch. The reviewer remained responsible for deciding whether the change
matched the assignment, kept the architecture coherent, and preserved existing behavior.

Legacy behavior was protected before refactoring. The original calculator contract stayed intact, and the observable
rounding and eligibility behavior remained covered by characterization tests. Architectural and business assumptions
were documented where requirements left room for interpretation.

Unnecessary abstractions were rejected in favor of the smallest mechanism that protected the invariant. For example,
monthly idempotency is enforced by PostgreSQL uniqueness plus atomic insert semantics rather than JVM locks, distributed
locks, or a larger distributed workflow.

No custom Codex Skill was used for this project. Project-specific constraints remain in version-controlled repository
files. Reusable cross-project procedures could be extracted into Codex Skills later, while business invariants should
remain in `AGENTS.md` and project documentation.

## Reproducing the Workflow

Another engineer can reproduce the AI-assisted workflow with the same repository harness:

1. Clone the repository.
2. Read `AGENTS.md` for persistent constraints and navigation.
3. Inspect the source-of-truth docs: `docs/architecture-guidelines.md`, `docs/openapi.yaml`, and `docs/erd.puml`.
4. Select a focused prompt under `docs/prompts/` that matches the task phase.
5. Apply a small change and review the diff.
6. Run focused tests for the touched behavior.
7. Run broader verification before accepting the change.

Useful commands:

```bash
cd kotlin
./mvnw clean test
./mvnw dependency-check:check
docker compose up --build
```

The commands above cover regression tests, security scanning, and a local runtime smoke path. They do not replace human
review of the API contract, architecture boundaries, and documented assumptions.

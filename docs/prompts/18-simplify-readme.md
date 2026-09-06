# 18 - Simplify README for the Reviewer

Review the final repository before editing. Read `README.md`, the original assignment, `AGENTS.md`, `docs/architecture-guidelines.md`, `docs/openapi.yaml`, `docs/erd.puml`, application configuration, Docker Compose, tests, and the final implementation.

## Goal

Rewrite `README.md` into a concise reviewer-first entry point. Target roughly 100-140 lines, preferably less if all important information fits. A technical interviewer should understand the project, run it, and identify the important engineering decisions in 3-5 minutes.

Do not change application code.

Optimize for these questions:

1. What was built?
2. How do I run it?
3. What are the two endpoints?
4. What architecture was chosen?
5. What engineering decisions are interesting?
6. How was correctness tested?
7. How was AI used?
8. What important assumptions were made?

## Target structure

```text
# XA Bank Time Deposit
short overview

## Quick Start
## API
## Architecture
## Key Design Decisions
## Testing
## AI-Assisted Development
## Assumptions
```

Do not add a table of contents unless genuinely needed.

## Quick Start

Keep only the reviewer-critical commands. Prefer the shortest verified path, for example:

```bash
cd kotlin
docker compose up --build
```

Include the actual verified Swagger URL and:

```bash
./mvnw clean test
```

Do not list every environment variable in README.

## API

Show exactly:

```text
GET  /time-deposits
POST /time-deposits/balances
```

One short description per endpoint. Reference `docs/openapi.yaml` as the public contract. Do not duplicate the contract.

## Architecture

Keep a small diagram:

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

Explain in 2-3 sentences. Mention that the protected legacy `TimeDeposit` and calculator contract/behavior remain unchanged. Do not enumerate every package.

## Key Design Decisions

This is the most valuable section. Keep approximately 5-7 high-signal bullets covering the final implementation:

- plan-specific interest policies / Strategy while preserving legacy behavior
- bounded page-based GET
- keyset batches for update-all
- bounded workers
- transaction per batch
- `days` controls eligibility
- monthly accrual period controls idempotency
- PostgreSQL unique constraint plus atomic `ON CONFLICT DO NOTHING`
- eligibility before claim
- database invariant avoids JVM/distributed locks

Combine related points. Example level of detail:

```markdown
- **Extensible interest calculation**: plan-specific policies isolate interest rules while preserving the original calculator contract and behavior.
- **Bounded reads**: `GET /time-deposits` uses page-based pagination instead of returning an unbounded dataset.
- **Bounded writes**: balance updates use keyset batches and a small worker pool rather than loading the complete table into memory.
- **Transaction per batch**: avoids a long-running global transaction while keeping failed batches safely retryable.
- **Idempotent accrual**: `days` determines eligibility; `(time_deposit_id, accrual_period)` provides monthly idempotency.
- **Database concurrency invariant**: eligible deposits are atomically claimed with PostgreSQL `INSERT ... ON CONFLICT DO NOTHING`, avoiding JVM/distributed locks.
```

Adapt to actual code. Never document unimplemented behavior.

## Testing

Reduce to about five bullets:

- legacy characterization and boundary cases
- REST API and pagination
- PostgreSQL persistence with Testcontainers
- concurrent accrual/idempotency
- transaction rollback and safe retry

Mention no H2 if still true. Do not enumerate Spring context startup internals.

## AI-Assisted Development

Keep only a short 5-8 line summary:

- OpenAI Codex used as engineering agent, not one-shot generator
- `AGENTS.md` provides persistent constraints
- `docs/prompts/` contains reproducible focused tasks
- contracts/docs/tests act as guardrails
- generated changes are verified

Point to `docs/ai-assisted-development.md` for details.

## Assumptions

Keep only reviewer-relevant ambiguities:

- Assignment says retrieve all; implementation intentionally uses bounded pagination for scalability.
- `days` determines business eligibility while accrual period provides monthly idempotency. Present this explicitly as a design interpretation, not an assignment fact.
- If useful, one sentence that update-all is atomic per batch, committed batches remain committed, and retry is safe.

## Remove or move out of README

Remove detailed:

- table of contents
- environment-variable catalog
- package-by-package tree
- database implementation prose
- logging/observability section
- OWASP workflow details and report filenames
- security remediation policy
- long seeding explanation
- Scope section listing Kafka/CQRS/Saga/etc.
- Hikari tuning explanation
- transaction/concurrency essays
- Spring/Testcontainers startup details

Preserve deeper information in appropriate `docs/` where useful.

## Style

Concise, technical, factual, senior-reviewer friendly. No marketing, no emojis, no giant prose blocks, no repeated information, no unverifiable claims.

## Consistency

Verify the rewritten README matches actual Java version, Maven commands, Docker command, Swagger URL, exactly two endpoints, GET pagination, update response, `docs/openapi.yaml`, PostgreSQL/Testcontainers, eligibility/idempotency semantics, and AI docs path.

Before editing report current line count, sections to keep/remove, and factual inconsistencies. After editing report new line count, information moved, confirmation no application code changed, and confirmation README matches actual behavior.

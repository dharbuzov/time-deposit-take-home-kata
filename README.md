# XA Bank Time Deposit

[![CI](https://github.com/dharbuzov/time-deposit-take-home-kata/actions/workflows/ci.yml/badge.svg)](https://github.com/dharbuzov/time-deposit-take-home-kata/actions/workflows/ci.yml)

Kotlin/Spring Boot implementation of the XA Bank Time Deposit take-home assignment.

The service exposes two business endpoints: one paginated read endpoint for time deposits and one command endpoint that
processes balance accruals for the current monthly period.

## Quick Start

Prerequisites:

- JDK 17
- Docker Desktop

Run the application with PostgreSQL:

```bash
cd kotlin
docker compose up --build
```

Compose starts PostgreSQL and the app, and enables deterministic synthetic demo data for review.

Run the full test suite:

```bash
cd kotlin
./mvnw clean test
```

GitHub Actions runs the same Maven test suite on push and pull request.

Run the application directly after PostgreSQL is available:

```bash
cd kotlin
./mvnw spring-boot:run
```

By default the app listens on port `8080` and connects to `jdbc:postgresql://localhost:5432/time_deposit` with
username/password `time_deposit`.

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## API

`docs/openapi.yaml` is the public REST contract and source of truth.

```text
GET  /time-deposits
POST /time-deposits/balances
```

- `GET /time-deposits`: returns one page of deposits with withdrawals. Defaults are `page=0`, `size=20`, and
  `sort=id,asc`; maximum `size` is `100`.
- `POST /time-deposits/balances`: processes eligible deposits for the current monthly accrual period and returns a
  summary with `period`, `processed`, `updated`, `alreadyProcessed`, and `notEligible`.

## Architecture

The implementation uses lightweight Hexagonal Architecture:

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

REST controllers stay thin and map HTTP requests to application use cases. Application services coordinate transaction
boundaries and persistence ports. Domain code owns interest rules and remains independent from Spring, HTTP, JPA, and
database details.

The shared legacy `TimeDeposit` class and `TimeDepositCalculator.updateBalance(xs: List<TimeDeposit>)` contract remain
in place, with characterization tests protecting observable behavior.

## Key Design Decisions

- **Extensible interest calculation**: plan-specific policies isolate Basic, Student, and Premium interest rules while
  preserving the legacy calculator behavior.
- **Bounded reads**: `GET /time-deposits` uses page-based pagination rather than returning an unbounded dataset.
- **Bounded writes**: update-all processing traverses deposit IDs with keyset batches and a fixed worker pool instead
  of loading the complete table into memory.
- **Transaction per batch**: each worker batch claims accruals and updates balances in one transaction, limiting
  rollback scope while keeping failed batches safely retryable.
- **Eligibility before idempotency claim**: `days` determines business eligibility before any monthly claim is attempted,
  so ineligible deposits do not consume the current period.
- **Monthly idempotency**: `(time_deposit_id, accrual_period)` prevents duplicate accrual for the same deposit and
  calendar month.
- **Database concurrency invariant**: PostgreSQL `INSERT ... ON CONFLICT DO NOTHING` protects the monthly claim, so no
  JVM or distributed lock is needed.

## Testing

- Characterization tests protect `TimeDepositCalculator.updateBalance` and the 30-day, 45-day, and 1-year boundaries.
- Interest policy unit tests cover plan-specific eligibility and monthly interest amounts.
- REST integration tests cover the two endpoints, pagination, response shape, and correlation ID behavior.
- PostgreSQL/Testcontainers integration tests cover persistence, Flyway migrations, update batching, concurrency, and
  idempotent accrual claims.
- Rollback and retry tests verify that failed batch work can be retried without duplicating already committed accruals.

## AI-Assisted Development

OpenAI Codex was used as an engineering agent, not as a one-shot generator.

`AGENTS.md` defines persistent repository constraints, while `docs/prompts/` contains focused reusable prompts for
implementation, review, and documentation tasks. The OpenAPI contract, architecture guide, ERD, and tests act as
guardrails for generated changes. Accepted changes are kept human-reviewable and verified with Maven tests.

See `docs/ai-assisted-development.md` for details.

## Assumptions

- The assignment asks to retrieve all time deposits; this implementation intentionally exposes bounded pagination to
  keep reads predictable as data grows.
- `days` is interpreted as the business eligibility input, while the calendar accrual period provides monthly
  idempotency.
- Update-all is atomic per worker batch. Committed batches remain committed if a later batch fails, and retry is safe
  because the monthly claim is database-enforced.

# XA Bank Time Deposit

Implementation of the XA Bank Time Deposit take-home assignment.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API](#api)
- [Assumptions / Design Decisions](#assumptions--design-decisions)
- [Architecture](#architecture)
- [Database](#database)
- [Testing](#testing)
- [Logging and Observability](#logging-and-observability)
- [Security](#security)
- [AI-Assisted Development](#ai-assisted-development)
- [Scope](#scope)

## Tech Stack

- Java 17
- Kotlin
- Spring Boot
- Maven with Maven Wrapper
- PostgreSQL
- Flyway
- Testcontainers
- Docker Compose
- OpenAPI 3

## Getting Started

The Maven project lives in `kotlin/`.

### Prerequisites

- JDK 17
- Docker, required for PostgreSQL and Testcontainers integration tests

### Run Tests

```bash
cd kotlin
./mvnw clean test
```

### Run Application Locally

Start PostgreSQL:

```bash
cd kotlin
docker compose up postgres
```

Run the application:

```bash
./mvnw spring-boot:run
```

The application listens on `SERVER_PORT`, default `8080`.
It reads database settings from environment variables:

- `SERVER_PORT`, default `8080`
- `DB_URL`, default `jdbc:postgresql://localhost:5432/time_deposit`
- `DB_USERNAME`, default `time_deposit`
- `DB_PASSWORD`, default `time_deposit`

### Run With Docker Compose

```bash
cd kotlin
docker compose up --build
```

Compose starts only the application and PostgreSQL.

## API

The application follows an API-first approach.

The OpenAPI contract is the source of truth for the public REST API:

`docs/openapi.yaml`

The contract defines exactly two business endpoints:

- `GET /time-deposits`
- `POST /time-deposits/balances`

Both endpoints are implemented as thin REST adapters over the application use cases.

Example calls:

```bash
curl "http://localhost:8080/time-deposits?page=0&size=20&sort=id,asc"
curl -X POST -i http://localhost:8080/time-deposits/balances
```

`POST /time-deposits/balances` returns `200 OK` with a processing summary when the update succeeds.

## Assumptions / Design Decisions

The original requirement asks to retrieve all time deposits. Returning an unbounded dataset in a single response
does not scale safely as the table grows, because it can cause excessive database reads, application memory usage,
large JSON payloads, and long response times. The GET endpoint therefore uses standard page-based pagination
(`page`, `size`, `sort`) with bounded page sizes. This is an intentional scalability trade-off that keeps the API
predictable while preserving access to the complete dataset across pages.

`offset` is not exposed separately because page-based pagination already derives it internally as `page * size`,
and exposing both would create ambiguous semantics.

### Bulk Balance Update Design

Updating every time deposit through an unbounded `findAll()` makes memory consumption and transaction size grow with
the complete table. The implementation therefore traverses deposit IDs in bounded keyset batches using an operation
start upper bound. Deposits inserted after the run starts are intentionally left for the next invocation.

Batches can be processed independently, so a small bounded worker pool improves throughput while keeping database
pressure controlled. The default worker count is deliberately below the default Hikari maximum pool size, leaving
connection headroom for request handling and other database work.

One transaction across the complete dataset would create a long-running transaction with large rollback scope and
prolonged resource retention. Each worker batch therefore executes in its own transaction. Successfully committed
batches remain committed if a later batch fails; a retry is safe because already committed eligible deposits are
rejected by the monthly unique claim while the failed batch's rolled-back claims are available again.

`days` is part of the supplied business rules and determines whether a deposit is currently eligible for interest. The
calculator applies a monthly interest amount (`annualRate / 12`), therefore idempotency is tracked separately by
calendar accrual period. `days` is not used as the idempotency key.

An ineligible deposit must not consume its monthly accrual slot. For example, a Premium deposit at day 45 is not
eligible, but at day 46 it becomes eligible. Therefore the system checks `days` eligibility first and only then attempts
the monthly claim.

Monthly processing metadata is infrastructure state rather than part of the protected `TimeDeposit` model. It is stored
in a dedicated technical table and does not modify the assignment's existing `timeDeposits` schema.

`UNIQUE(time_deposit_id, accrual_period)` is the atomic idempotency and concurrency invariant. It prevents duplicate
monthly application across repeated HTTP requests, concurrent worker threads, and multiple application instances. The
claim and balance change are part of the same batch transaction. If the batch fails, both are rolled back, allowing the
failed work to be safely retried.

Database uniqueness already provides the required cross-thread and cross-instance correctness, so JVM or distributed
locks would add complexity without improving the invariant.

## Architecture

The target architecture is lightweight Hexagonal Architecture:

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

The source tree now contains the initial architecture skeleton under `org.ikigaidigital`:

- `domain` for Spring/JPA-free business-facing models
- `application.observability` for lightweight use-case logging support
- `application.port.in` for the two use-case contracts
- `application.port.out` for the persistence boundary used by application services
- `application.service` for minimal use-case skeletons
- `adapter.in.rest` for API response DTOs and mapping boundaries
- `adapter.out.persistence` for JPA persistence entities, repositories, mapping, and the outbound adapter

REST controllers are thin inbound adapters and delegate to application use cases.
The protected legacy `TimeDeposit` and `TimeDepositCalculator.updateBalance` contract remains unchanged.

## Database

PostgreSQL is the runtime database.

Flyway manages schema migrations. The initial migration creates:

- `timeDeposits` with `id`, `planType`, `days`, and `balance`
- `withdrawals` with `id`, `timeDepositId`, `amount`, and `date`

The schema is based on `docs/erd.puml`.

The outbound persistence adapter uses Spring Data JPA with persistence-specific entities.
JPA mappings explicitly preserve the required mixed-case table and column names from the assignment.
Updating all time deposit balances is coordinated by the application service and executed with one transaction per
bounded worker batch.

## Testing

The test suite contains:

- Characterization tests protecting existing `TimeDepositCalculator.updateBalance` behavior
- Persistence adapter integration tests using PostgreSQL Testcontainers and Flyway
- Application use-case integration tests covering get-all, update-all, and rollback behavior
- API integration tests covering the two REST endpoints
- E2E tests covering HTTP-to-PostgreSQL flows through the real Spring application context
- A minimal Spring Boot/Testcontainers integration test that verifies:
  - Spring context startup
  - PostgreSQL container startup
  - database connectivity
  - Flyway migration execution

H2 is not used as a persistence substitute.

PostgreSQL integration and E2E tests run against real PostgreSQL containers and require Docker.
If Docker is unavailable, those tests are skipped by Testcontainers.

## Logging and Observability

The application uses Spring Boot's default SLF4J/Logback logging stack with console output.

HTTP requests support `X-Correlation-ID`:

- an incoming non-blank value is reused
- a missing or blank value is replaced with a generated UUID
- the value is returned in the `X-Correlation-ID` response header
- the value is included in application logs for the request

The update-all balance operation logs one concise summary with operation name, processed deposit count,
duration, and success or failure status. No external observability stack is required.

## Security

Security is treated as part of the regular build workflow.

Dependency hygiene is checked with OWASP Dependency-Check:

```bash
cd kotlin
./mvnw dependency-check:check
```

The generated reports are written under `kotlin/target/`, including:

- `kotlin/target/dependency-check-report.html`
- `kotlin/target/dependency-check-report.json`

Use Maven's dependency tree to identify whether a finding comes from a direct dependency,
a transitive dependency, a test-only dependency, or a build/plugin dependency:

```bash
cd kotlin
./mvnw dependency:tree
```

High and Critical findings must be reviewed explicitly. Prefer Spring Boot BOM-managed
versions and compatible patch/minor upgrades before adding narrow dependency overrides.
Suppressions are allowed only for confirmed false positives or non-applicable findings,
and each suppression must include a documented technical justification.

Runtime credentials and database configuration are supplied through environment variables.
Do not commit secrets, tokens, private keys, or production credentials.

## AI-Assisted Development

OpenAI Codex is used as an engineering assistant. Repository-level instructions are defined in `AGENTS.md`, and reusable prompts are stored under `docs/prompts/`.

AI-generated code is treated as a proposal and should remain understandable, testable, and maintainable by a human engineer.

## Scope

The solution intentionally avoids speculative distributed-system complexity that is not required by the assignment.

Patterns such as distributed locks, Kafka, CQRS, event sourcing, sharding, Saga, TCC, and 2PC should only be introduced when supported by a concrete requirement.

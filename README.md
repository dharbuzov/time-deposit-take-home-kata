# XA Bank Time Deposit

Implementation of the XA Bank Time Deposit take-home assignment.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API](#api)
- [Architecture](#architecture)
- [Database](#database)
- [Testing](#testing)
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
curl http://localhost:8080/time-deposits
curl -X POST -i http://localhost:8080/time-deposits/balances
```

`POST /time-deposits/balances` returns `204 No Content` when the update succeeds.

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
Updating all time deposit balances is orchestrated by the application service inside one Spring transaction.

## Testing

The test suite contains:

- Characterization tests protecting existing `TimeDepositCalculator.updateBalance` behavior
- Persistence adapter integration tests using PostgreSQL Testcontainers and Flyway
- Application use-case integration tests covering get-all, update-all, and rollback behavior
- API integration tests covering the two REST endpoints
- A minimal Spring Boot/Testcontainers integration test that verifies:
  - Spring context startup
  - PostgreSQL container startup
  - database connectivity
  - Flyway migration execution

H2 is not used as a persistence substitute.

The Testcontainers bootstrap test runs against real PostgreSQL when Docker is available.
If Docker is unavailable, that infrastructure test is skipped by Testcontainers.

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

# XA Bank Time Deposit

Implementation of the XA Bank Time Deposit take-home assignment.

## Tech Stack

- Kotlin
- Spring Boot
- Maven
- PostgreSQL
- Flyway
- Testcontainers
- OpenAPI 3

## Getting Started

### Prerequisites

- JDK 21+
- Docker

### Run Tests

```bash
./mvnw test
```

### Run Application

```bash
./mvnw spring-boot:run
```

## API

The application follows an **API-first** approach.

The OpenAPI contract is the source of truth for the public REST API and is available at:

`docs/openapi.yaml`

The application exposes exactly two business endpoints.

### Get All Time Deposits

```http
GET /time-deposits
```

Example response:

```json
[
  {
    "id": 1,
    "planType": "PREMIUM",
    "balance": 1050.00,
    "days": 60,
    "withdrawals": [
      {
        "id": 10,
        "amount": 100.00,
        "date": "2026-09-01"
      }
    ]
  }
]
```

### Update All Time Deposit Balances

```http
POST /time-deposits/balances
```

Successful response:

```text
204 No Content
```

## Architecture

The application uses a lightweight **Hexagonal Architecture**.

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

Business logic is kept independent from HTTP, Spring, JPA, and persistence implementation details.

Architecture guidelines:

`docs/architecture-guidelines.md`

Database model:

`docs/erd.puml`

## Interest Rules

| Plan | Interest | Conditions |
| --- | ---: | --- |
| Basic | 1% | No interest during the first 30 days |
| Student | 3% | No interest during the first 30 days and no interest after 1 year |
| Premium | 5% | Interest starts after 45 days |

The existing `TimeDepositCalculator.updateBalance` public contract and observable behavior are preserved.

## Design Decisions

### API First

The OpenAPI contract is defined before the REST implementation.

Changes to the public API should be reflected in `docs/openapi.yaml` before changing the corresponding adapter code.

### Interest Calculation

Interest calculation is designed around explicit policies so that plan-specific rules remain isolated and future plan types can be introduced with minimal changes to existing business logic.

### Money

Monetary values use `BigDecimal`.

`Double` and `Float` are not used for balances, withdrawals, or interest calculations.

### Transactions and Concurrency

Updating all balances is treated as a single application use case with a clear transaction boundary.

Database consistency guarantees are preferred over application-level locking. Additional locking or distributed coordination should only be introduced when a concrete concurrency invariant requires it.

### Persistence

PostgreSQL is used as the relational database.

Schema changes are managed through Flyway migrations.

Integration tests use PostgreSQL through Testcontainers so that persistence behavior is tested against the same database engine used by the application.

## Testing

The testing strategy includes:

- Characterization tests protecting existing behavior
- Unit tests for domain and interest rules
- Persistence integration tests using Testcontainers
- API integration tests for the two required endpoints

Business boundary cases receive explicit coverage, particularly around:

- 30 days
- 45 days
- 1 year

Run the complete test suite with:

```bash
./mvnw test
```

## Assumptions

Where requirements are ambiguous, the smallest behavior-compatible interpretation is preferred.

Assumptions should be documented close to the relevant implementation or test.

No additional business API functionality is introduced beyond the assignment requirements.

## AI-Assisted Development

OpenAI Codex is used as an engineering assistant throughout the exercise.

Repository-level instructions are defined in:

`AGENTS.md`

Reusable prompts and workflow examples are stored under:

`docs/prompts/`

Codex is used for:

- repository analysis and onboarding
- identifying characterization test scenarios
- implementation assistance
- test generation and review
- refactoring suggestions
- final requirement compliance review

The workflow for significant changes is:

```text
Analyze
  ↓
Plan
  ↓
Characterize Existing Behavior
  ↓
Implement / Refactor
  ↓
Test
  ↓
Review
```

Architectural decisions, assumptions, and AI-generated changes are reviewed manually before being accepted.

AI-generated code is treated as a proposal rather than authoritative implementation.

## Documentation

```text
docs/
├── architecture-guidelines.md
├── erd.puml
├── openapi.yaml
└── prompts/
```

## Scope

The solution intentionally avoids speculative distributed-system complexity that is not required by the assignment.

Patterns such as distributed locks, Kafka, CQRS, event sourcing, sharding, Saga, TCC, and 2PC should only be introduced when supported by a concrete requirement.

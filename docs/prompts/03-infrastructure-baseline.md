# 03 - Infrastructure Baseline

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- `docs/erd.puml`

Inspect the current Kotlin/Maven project before making changes.

Assume characterization tests are already in place.

## Goal

Convert the current plain Kotlin/Maven project into a minimal, reproducible Spring Boot application baseline with PostgreSQL, Flyway, Testcontainers, Docker, and Maven Wrapper.

Do not implement business API logic yet.
Do not refactor `TimeDepositCalculator` yet.
Do not change existing business behavior.

## 1. Runtime and Build

Preserve the existing Java 17 baseline unless a required dependency creates a concrete technical incompatibility.

Select mutually compatible stable versions of:

- Java
- Kotlin
- Spring Boot
- Maven plugins

Do not upgrade Java or Kotlin merely for modernization.

Keep Maven.

Do not migrate to Gradle.

## 2. Maven Wrapper

Add Maven Wrapper to the existing Maven project.

The project must be buildable without requiring globally installed Maven.

Expected commands:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

Include all required Maven Wrapper files in the repository.

Use `./mvnw` in documentation instead of relying on `mvn`.

## 3. Spring Boot

Convert the existing project into the minimum Spring Boot application required for the assignment.

Add only necessary dependencies for:

- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- Testcontainers
- JUnit 5

Add the minimal Spring Boot application entry point.

Do not add controllers or business services yet.

## 4. PostgreSQL

Use PostgreSQL as the application database.

Configure database connectivity through environment variables where practical.

Keep local configuration simple and reproducible.

Do not introduce an in-memory production database.

## 5. Flyway

Add Flyway for database schema management.

Create the initial migration from the schema defined in `README.md`.

The README schema is authoritative for persistence naming and structure.

Do not rename tables or columns for style or convention.

Preserve exactly the required tables and fields from the assignment/README, including names such as:

- `timeDeposits`
- `withdrawals`
- `planType`
- `timeDepositId`

Do not convert required camelCase names to snake_case.

Use `docs/erd.puml` only as supporting documentation. If it conflicts with `README.md`, fix the ERD/documentation rather than changing the required database schema.

Do not add speculative columns, tables, indexes, version fields, timestamps, statuses, or other persistence fields unless explicitly required.

Application startup should validate/apply migrations through Flyway.

## 6. Testcontainers

Add PostgreSQL Testcontainers support for integration testing.

Integration tests must use a real PostgreSQL container.

Prefer:

- real PostgreSQL
- real Flyway migrations
- real Spring persistence configuration

Do not use H2 as a substitute for persistence integration tests.

Keep unit and characterization tests independent from Testcontainers.

Add a minimal infrastructure/bootstrap integration test proving that:

- Spring application context starts
- PostgreSQL container starts
- database connectivity works
- Flyway migrations execute successfully

Do not test business functionality in this infrastructure test.

## 7. Docker

Add:

- `Dockerfile`
- `compose.yaml`

Docker Compose should contain only the infrastructure required for this application:

- application
- PostgreSQL

Use environment variables for runtime configuration.

Keep the setup minimal.

Do not add:

- Kafka
- Redis
- distributed cache
- message brokers
- observability stack
- additional infrastructure

unless explicitly required by the assignment.

The expected local workflow should be simple:

```bash
docker compose up --build
```

## 8. Documentation

Update documentation only to reflect infrastructure that actually exists and works.

### README.md

Update:

- Tech Stack
- Prerequisites
- Getting Started
- Maven Wrapper commands
- Docker Compose startup
- PostgreSQL
- Flyway
- Testcontainers
- Testing

Add a Table of Contents near the top.

Remove or correct documentation that does not match the actual implementation.

In particular, do not document Java 21 if the project remains on Java 17.

### Architecture Guidelines

Update:

`docs/architecture-guidelines.md`

Document concisely:

- PostgreSQL as persistence infrastructure
- Flyway for migrations
- Testcontainers for persistence integration tests
- Docker Compose for local runtime
- Maven Wrapper for reproducible builds
- infrastructure remains outside the domain
- unit tests must not require infrastructure

Do not duplicate the entire README.

## 9. Verification

After implementation:

1. Run:

```bash
./mvnw clean test
```

2. Verify characterization tests still pass.

3. Verify the Spring application context starts.

4. Verify the PostgreSQL Testcontainer starts.

5. Verify Flyway migrations execute successfully.

6. Validate Docker Compose configuration.

7. If possible, run:

```bash
docker compose up --build
```

8. Verify documentation commands match the actual project.

## Constraints

- Treat `README.md` as the source of truth for assignment requirements.
- Do not rename or redesign the required database tables or columns.
- Preserve the existing Java 17 baseline unless technically incompatible.
- Keep Maven.
- Add Maven Wrapper.
- Do not migrate to Gradle.
- Do not change the public `TimeDeposit` contract.
- Do not change `TimeDepositCalculator.updateBalance` signature.
- Do not change existing calculator behavior.
- Do not implement REST endpoints yet.
- Do not implement business use cases yet.
- Do not refactor interest calculation yet.
- Do not add speculative infrastructure.
- Prefer the smallest working solution.

## Output

Before editing, provide a short implementation plan.

After implementation, report:

1. Files changed
2. Versions selected
3. Dependencies added
4. Maven Wrapper setup
5. Infrastructure added
6. Flyway migrations added
7. Tests executed and results
8. Docker verification
9. Documentation updated
10. Assumptions or remaining issues

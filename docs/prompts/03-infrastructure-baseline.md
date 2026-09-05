# 03 - Infrastructure Baseline

Read:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yml`
- `docs/erd.puml`

Assume characterization tests are already in place.

## Goal

Convert the current plain Kotlin/Maven project into a minimal Spring Boot application with reproducible local and integration-test infrastructure.

Do not implement business API logic yet.
Do not refactor `TimeDepositCalculator` yet.

## Required Changes

### Build

Update the existing Maven project.

Add only required dependencies for:

- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- Testcontainers
- JUnit 5

Keep Maven.
Do not migrate to Gradle.

Prefer Java 17 unless there is a strong reason to change the project baseline.

### Spring Boot

Add the minimal Spring Boot application entry point.

Do not add controllers or business services yet.

### PostgreSQL

Add PostgreSQL configuration for local runtime.

Use environment variables for database connection where practical.

### Flyway

Add an initial Flyway migration matching `docs/erd.puml`.

Do not add extra columns unless required.

### Testcontainers

Add PostgreSQL Testcontainers support for integration tests.

Ensure integration tests can start the application against a real PostgreSQL container.

Do not use H2 as a substitute.

### Docker

Add:

- `Dockerfile`
- `compose.yaml`

The Compose setup should run:

- application
- PostgreSQL

Keep it minimal.

Do not add Redis, Kafka, or other infrastructure.

### Maven Wrapper

If practical, add Maven Wrapper so the project can be built without requiring a globally installed Maven installation.

## Verification

After changes:

1. Compile the project.
2. Run unit tests.
3. Run integration/bootstrap test with Testcontainers.
4. Validate Flyway migration startup.
5. Validate Docker Compose configuration if the environment allows.

## Constraints

- Preserve `TimeDeposit`.
- Preserve `TimeDepositCalculator.updateBalance`.
- Do not change business behavior.
- Do not add REST endpoints yet.
- Do not add speculative architecture.
- Keep the infrastructure minimal and reproducible.

## Output

Before editing, provide a short plan.

After editing, report:

1. files changed
2. dependencies added
3. infrastructure added
4. tests executed
5. remaining issues
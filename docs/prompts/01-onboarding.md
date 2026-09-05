# 01 - Repository Onboarding

You are joining this repository as a new engineer.

Read these files first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- `docs/erd.puml`

Then inspect:

- the Kotlin project structure
- `pom.xml`
- existing production code
- existing tests
- existing configuration
- existing database-related code

Do not modify any files yet.

## Goal

Understand the current implementation, protected behavior, requirement gaps, and the safest implementation path before changing anything.

## Provide

### 1. Current Implementation

Summarize:

- what the application currently does
- main domain classes
- current interest calculation behavior
- current API state
- current persistence/database state
- current test coverage

### 2. Protected Behavior

Identify:

- behavior that must remain unchanged
- public classes or method signatures that are constrained
- existing tests that appear to define expected behavior

Pay special attention to:

- `TimeDeposit`
- `TimeDepositCalculator.updateBalance`

### 3. Requirement Gaps

Compare the current codebase against `README.md` and identify what is still missing.

Include:

- two required REST endpoints
- OpenAPI contract alignment
- database persistence
- withdrawals relationship
- interest rules
- Hexagonal Architecture expectations
- Testcontainers
- Docker/local runtime
- Flyway migrations
- AI-assisted development documentation

### 4. Risks and Ambiguities

Identify:

- unclear business rules
- boundary conditions
- behavior that should be characterized before refactoring
- potential persistence or transaction concerns
- potential concurrency concerns

Do not invent requirements.

### 5. Architecture Assessment

Evaluate whether the current code aligns with:

- lightweight Hexagonal Architecture
- API-first development
- domain independence from framework concerns
- clear application/use-case boundaries
- persistence behind outbound ports

Recommend only changes that solve a concrete problem.

### 6. Build and Infrastructure Assessment

Inspect the current Maven/Kotlin setup and report:

- Java/Kotlin versions
- Spring Boot version
- current dependencies
- current test setup
- whether PostgreSQL support exists
- whether Flyway exists
- whether Docker support exists
- whether Testcontainers exists

Do not migrate from Maven to Gradle.

### 7. Proposed Implementation Plan

Provide an ordered, minimal plan.

Prefer this general sequence where appropriate:

1. Characterize existing behavior
2. Lock down business boundaries with tests
3. Define/verify API contract
4. Refactor domain design
5. Add application/use-case layer
6. Add persistence
7. Add Testcontainers
8. Add Docker/local runtime
9. Add API implementation
10. Update documentation
11. Final compliance review

Adjust the sequence if the repository structure suggests a safer approach.

## Constraints

- Do not modify code yet.
- Do not add dependencies yet.
- Do not change business behavior.
- Do not change `TimeDepositCalculator.updateBalance` signature.
- Do not introduce breaking changes to `TimeDeposit`.
- Do not add extra API endpoints.
- Do not overengineer.
- Prefer the smallest design that fully satisfies the assignment.

## Output Format

Return:

1. Current state
2. Protected behavior
3. Missing requirements
4. Risks / ambiguities
5. Architecture assessment
6. Build / infrastructure assessment
7. Recommended implementation plan
# AGENTS.md

## Start Here

Before making any changes:

1. Read `README.md`.
2. Read `docs/architecture-guidelines.md`.
3. Read `docs/openapi.yaml`.
4. Inspect `docs/erd.puml`.
5. Inspect the existing implementation and tests.
6. Propose a short plan before making significant changes.

Reusable AI prompts are stored under `docs/prompts/`.

Repository files are the source of truth. Do not rely on previous conversational context.

## Core Constraints

- Do not introduce breaking changes to the shared `TimeDeposit` class.
- Do not change the `TimeDepositCalculator.updateBalance` method signature.
- Preserve the observable behavior of `TimeDepositCalculator.updateBalance`.
- Expose exactly two business REST endpoints.
- Do not add functionality outside the assignment scope.
- Avoid unrelated refactoring.

## Development Workflow

For non-trivial changes:

1. Analyze requirements and existing behavior.
2. Identify invariants, boundaries, and ambiguities.
3. Add or verify characterization tests where needed.
4. Propose the smallest viable design.
5. Implement incrementally.
6. Run relevant tests.
7. Review against `README.md`, `docs/openapi.yaml`, and architecture guidelines.
8. Remove unnecessary complexity.

Prefer behavior-preserving refactoring over rewrites.

## API First

Use an API-first approach.

Before implementing or changing REST functionality:

1. Define or update `docs/openapi.yaml`.
2. Review HTTP semantics and request/response schemas.
3. Confirm the contract exposes exactly two business endpoints.
4. Implement the REST adapter against the contract.
5. Keep API DTOs separate from persistence entities.

`docs/openapi.yaml` is the source of truth for the public REST API.

Do not change public endpoint behavior in code without updating the contract first.

Public collection APIs should use bounded pagination for potentially large datasets. Treat `page`, `size`, and
`sort` as API concerns, derive raw database `offset` internally from `page * size`, keep framework pagination
types inside adapters, use framework-independent pagination models in application ports, and avoid unbounded
`findAll()` access at REST boundaries.

## Architecture

Follow `docs/architecture-guidelines.md`.

In particular:

- Use lightweight Hexagonal Architecture.
- Keep domain logic independent from Spring, HTTP, JPA, and database details.
- Keep transaction boundaries explicit at the application/use-case level.
- Prefer database guarantees over application-level coordination.
- Do not introduce locks without identifying the invariant and race condition they protect.
- Do not use JVM locks for cross-instance consistency.
- Prefer optimistic locking before pessimistic locking when conflicts are rare.
- Do not introduce distributed locks unless database-level coordination is insufficient.
- Design for the stated scale, not hypothetical internet scale.
- Do not introduce Kafka, CQRS, event sourcing, sharding, distributed cache, microservices, Saga, TCC, or 2PC without a concrete requirement.

## Utilities and Mappers

- Avoid global `utils` packages. Place helpers with the layer or concept that owns them.
- Only truly generic, dependency-free helpers belong in shared code.
- Keep persistence mapping inside the persistence adapter.
- Keep REST mapping inside the inbound REST adapter.
- Keep business calculations and business-specific helpers in the domain.

## Engineering Rules

- Use `BigDecimal` for all monetary values.
- Never use `Double` or `Float` for money.
- Prefer simple, readable Kotlin over clever syntax.
- Use constructor injection.
- Prefer immutable values where practical.
- Keep classes and functions focused.
- Make dependencies explicit.
- Avoid speculative abstractions and unnecessary frameworks.
- Do not silently change business behavior to make tests pass.
- Document assumptions when requirements are ambiguous.

## Atomicity and Concurrency

For state-changing operations:

1. Identify the business invariant.
2. Define the transaction boundary.
3. Determine whether concurrent execution can violate the invariant.
4. Use the simplest database consistency mechanism that protects it.
5. Add concurrency-specific mechanisms only when justified.

Avoid partial updates when the business operation is expected to succeed or fail as one unit.

Keep lock scope minimal. Never hold locks across unnecessary slow or external operations.

## Persistence

- Keep persistence concerns outside the domain.
- Keep database schema synchronized with `docs/erd.puml`.
- Prefer explicit migrations.
- Do not expose JPA entities or Spring Data repositories through domain/application boundaries.
- Do not add schema fields without a concrete requirement.

## Testing

Prefer:

- Characterization tests for existing behavior.
- Unit tests for domain and interest rules.
- Testcontainers for database integration tests.
- Integration tests for the two required API endpoints.

Test business boundaries explicitly, especially around:

- 30 days
- 45 days
- 1 year

For concurrency-sensitive behavior, test the business invariant rather than implementation details.

Run the full test suite before considering a task complete.

## Build and Dependencies

Use the existing build system unless changing it solves a concrete problem.

Do not migrate build tools or add dependencies for stylistic reasons.

Before adding a dependency, verify that the same result cannot be achieved clearly with the existing stack.

## Security First

Security is part of the normal engineering workflow.

- Never commit credentials, tokens, secrets, or private keys.
- Do not hardcode production credentials; use environment-based configuration.
- Minimize new dependencies and inspect dependency impact when adding or upgrading libraries.
- Prefer Spring Boot/BOM-managed dependency versions.
- Review High and Critical vulnerability findings explicitly.
- Do not suppress findings without documented technical justification.
- Run the Maven security scan after dependency or build changes.
- Preserve characterized business behavior during security fixes.
- Validate untrusted input at system boundaries where applicable.
- Use safe, parameterized persistence access.
- Do not expose secrets or unnecessary internal exception details through APIs or logs.
- Use least privilege where practical.
- Do not introduce heavyweight security infrastructure without a concrete requirement.

Run security and regression checks from `kotlin/`:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
./mvnw clean test
```

## Logging and Observability

- Use the SLF4J/Logback stack already provided by Spring Boot.
- Log business operation outcomes, not internal noise.
- Include `X-Correlation-ID` in HTTP request context and log output.
- Never log secrets, credentials, tokens, private keys, or database URLs containing credentials.
- Avoid logging full domain or persistence objects.
- Avoid duplicate exception logging across layers; log where useful operational context exists.
- Keep INFO logs useful for operations and DEBUG logs for diagnostics.
- Do not add observability infrastructure without a concrete requirement.
- Preserve the Security First rules when adding or changing logs.

## Documentation

Keep documentation lightweight and synchronized with the implementation:

- `README.md` - solution overview, setup, decisions, and AI usage
- `docs/architecture-guidelines.md` - architecture and engineering rules
- `docs/openapi.yaml` - public API contract
- `docs/erd.puml` - persistence model
- `docs/prompts/` - reusable AI workflow prompts

Do not document speculative functionality.

## AI-Assisted Development

OpenAI Codex is used as an engineering assistant.

For significant changes:

1. Inspect relevant files first.
2. Explain the intended change and assumptions.
3. Keep implementation scoped to the requirement.
4. Run tests after implementation.
5. Report trade-offs and unresolved risks.
6. Perform a final requirement compliance review.

AI-generated code is a proposal. Keep every accepted change understandable, testable, and maintainable by a human engineer.

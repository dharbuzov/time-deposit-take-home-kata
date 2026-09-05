# 06 - Hexagonal Architecture Skeleton

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- `docs/erd.puml`
- current Kotlin source tree
- current tests
- current Spring Boot configuration

Inspect the repository before making changes.

Assume:

- characterization tests are already in place
- infrastructure baseline is complete
- OpenAPI contract is aligned
- security baseline is complete

## Goal

Introduce a minimal hexagonal architecture skeleton that cleanly separates:

- domain
- application/use cases
- inbound REST adapter
- outbound persistence adapter

This step is structure-first.

Do not implement the full business flow yet.
Do not refactor the legacy interest calculation yet.
Do not add extra endpoints.
Do not change the persistence schema.
Do not break existing legacy behavior.

## Architecture Direction

Use a lightweight hexagonal structure similar to:

```text
domain/
application/
  port/
    in/
    out/
  service/
adapter/
  in/
    rest/
  out/
    persistence/
```

Keep package names idiomatic for the existing project.

Do not create unnecessary layers or abstractions.

## 1. Preserve Legacy Contract

The existing shared `TimeDeposit` contract is protected.

Do not change its public shape.

Do not change:

`TimeDepositCalculator.updateBalance`

signature or observable behavior.

Do not force the legacy `TimeDeposit` class to become:

- a JPA entity
- a REST response model
- a persistence DTO
- a framework-specific object

Keep legacy compatibility isolated from new architecture.

## 2. Domain

Create only the domain abstractions that are clearly required.

The domain layer must:

- contain business concepts
- avoid Spring annotations
- avoid JPA annotations
- avoid REST concerns
- avoid infrastructure dependencies

Do not over-model the domain before the behavior is implemented.

Do not introduce speculative aggregates, factories, events, value objects, or domain services without a concrete reason.

If a new domain representation is needed, keep it minimal and document how it relates to the protected legacy `TimeDeposit`.

## 3. Inbound Application Ports

Create application use-case contracts for exactly the two assignment capabilities.

### Get all time deposits

Define an inbound port representing retrieval of all time deposits.

Example intent:

```text
GetTimeDepositsUseCase
```

The exact name may be adjusted to fit Kotlin style.

### Update all balances

Define an inbound port representing the operation that updates balances of all persisted time deposits.

Example intent:

```text
UpdateTimeDepositBalancesUseCase
```

Do not expose HTTP types in application ports.

Do not bind use-case interfaces to Spring MVC.

## 4. Outbound Persistence Port

Create the minimum outbound port required by the application layer to work with persisted time deposits.

The application layer must not depend directly on Spring Data/JPA repositories.

The port should express business/application needs, not generic persistence CRUD.

Avoid broad interfaces such as:

```text
save()
findAll()
delete()
update()
```

unless they are genuinely required by the current use cases.

Prefer intention-revealing operations.

Do not implement the persistence adapter in full in this step unless a minimal stub/wiring is required for compilation.

## 5. Application Services

Create minimal application service skeletons implementing the inbound ports.

At this stage:

- establish dependency direction
- use constructor injection
- depend on outbound ports
- keep business logic minimal
- do not duplicate legacy calculator logic
- do not invent transaction behavior yet unless required for wiring

Do not implement full update-all orchestration yet.

If methods cannot be meaningfully implemented without the next persistence/business step, keep the skeleton minimal rather than adding fake behavior.

Do not return placeholder production data.

## 6. REST Adapter Skeleton

Create the inbound REST package structure required for the future two endpoints.

Do not implement endpoint business behavior in this step.

You may create:

- API DTO classes
- request/response mapping boundaries
- controller skeletons only if they can remain non-functional without violating tests

Prefer not to expose unfinished endpoints.

If adding controllers would make the application expose incomplete behavior, defer actual controllers to the REST implementation step.

## 7. API DTO / Read Model

The REST response must not reuse persistence entities directly.

Create a dedicated API/read DTO model if needed for the GET response.

It must be capable of representing the OpenAPI response:

- `id`
- `planType`
- `balance`
- `days`
- `withdrawals`

The withdrawal representation should contain only fields required by the contract.

Do not modify legacy `TimeDeposit` merely to include withdrawals.

Do not use persistence entities as REST DTOs.

## 8. Persistence Adapter Skeleton

Create the package/module boundary for:

```text
adapter/out/persistence
```

If needed for compilation, create only minimal adapter interfaces/classes.

Do not yet implement full JPA entity mapping, repository queries, or database behavior. That belongs in the next persistence step.

Do not change Flyway schema in this step.

## 9. Dependency Direction

Enforce this dependency direction:

```text
REST adapter
    ↓
application / inbound ports
    ↓
domain

application
    ↓
outbound ports

persistence adapter
    ↓
outbound ports
```

The domain must not depend on:

- Spring
- JPA
- REST
- PostgreSQL
- Testcontainers

The application layer should not depend on adapter implementation classes.

## 10. Spring Wiring

Use constructor injection.

Keep Spring-specific wiring at the application/adapter boundary.

Do not use field injection.

Avoid excessive `@Component` / `@Service` annotations if explicit configuration is clearer.

Do not introduce a dependency injection abstraction beyond Spring.

## 11. Naming and Complexity

Prefer names that describe intent.

Avoid architecture ceremony such as:

- BaseUseCase
- GenericRepository
- AbstractAdapter
- CommonService
- BaseEntity
- BaseController
- marker interfaces
- generic CRUD layers

unless there is a demonstrated need.

The assignment is small.

The architecture should make future change easier without making the current solution harder to understand.

## 12. Tests

Keep all existing tests green.

Add lightweight architecture/unit tests only where they provide real value.

Do not add large architecture-test frameworks solely for this step.

If application service skeletons contain behavior, add focused unit tests.

Do not introduce integration tests for persistence behavior yet.

## 13. Documentation

Update:

- `README.md`
- `docs/architecture-guidelines.md`

only where necessary to reflect the architecture that actually exists.

Document the package structure and dependency direction concisely.

Do not claim persistence or REST behavior that is not implemented yet.

Do not modify the OpenAPI contract in this step.

## Verification

After implementation:

1. Run:

```bash
./mvnw clean test
```

2. Confirm characterization tests still pass.

3. Confirm Spring application context still starts.

4. Confirm domain classes contain no Spring/JPA annotations.

5. Confirm application ports do not depend on REST or persistence framework types.

6. Confirm persistence adapter depends inward on the outbound port.

7. Confirm no extra REST endpoint was added.

8. Confirm database schema was not changed.

9. Confirm legacy calculator behavior was not changed.

## Constraints

- Keep the architecture lightweight.
- Exactly two business use cases.
- Do not change `TimeDeposit`.
- Do not change `TimeDepositCalculator.updateBalance` signature.
- Do not change legacy calculator behavior.
- Do not change `docs/openapi.yaml`.
- Do not change required database table or column names.
- Do not implement full JPA persistence yet.
- Do not refactor interest calculation yet.
- Do not add speculative abstractions.
- Do not add generic CRUD architecture.
- Prefer constructor injection.
- Preserve the Security First repository rules.
- Run security checks if dependencies/build configuration are changed.

## Output

Before editing, provide a short architecture plan.

After implementation, report:

1. Files/packages added
2. Domain abstractions introduced
3. Inbound ports introduced
4. Outbound ports introduced
5. Application service skeletons introduced
6. API/read DTOs introduced
7. Persistence adapter skeleton introduced
8. Spring wiring added
9. Tests added or changed
10. Test results
11. Documentation updates
12. Confirmation that API contract, database schema, and legacy behavior were unchanged
13. Any design decisions intentionally deferred to the next step

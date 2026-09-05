# 14 - Demo Data Seeding

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- current Spring Boot configuration
- current persistence adapter
- current Flyway migrations
- current Docker Compose configuration
- current tests

Inspect the repository before making changes.

Assume:

- characterization tests are complete
- infrastructure baseline is complete
- OpenAPI contract is aligned
- security baseline is complete
- hexagonal architecture is in place
- persistence adapter is complete
- interest refactoring is complete
- application use cases are complete
- REST adapters are complete
- logging/observability baseline is complete
- utility/shared-code cleanup is complete
- integration/E2E testing is complete

## Goal

Add optional local/demo database seeding so a reviewer can start the application and immediately exercise the two API endpoints with meaningful data.

The seeding must be disabled by default and must not be part of the production schema migration path.

Do not change business behavior.
Do not change API contracts.
Do not change required database schema.

## 1. Configuration Property

Introduce a dedicated property:

```yaml
app:
  database:
    seed:
      enabled: false
```

Default must be:

```text
false
```

The application must behave exactly as before when seeding is disabled.

For a local/demo profile or Docker Compose configuration, it is acceptable to enable:

```text
APP_DATABASE_SEED_ENABLED=true
```

Do not enable demo seeding globally.

## 2. Seeder Ownership

Place the seeder with the persistence adapter because it creates persistence data.

Prefer a package such as:

```text
adapter/out/persistence/seed/
```

Example:

```text
TimeDepositDataSeeder.kt
```

Do not place it in:

```text
domain/
application/
utils/
shared/
```

The seeder is infrastructure/demo behavior.

## 3. Conditional Activation

Use a small Spring conditional configuration mechanism.

Prefer:

```kotlin
@ConditionalOnProperty(
    prefix = "app.database.seed",
    name = ["enabled"],
    havingValue = "true"
)
```

The seeder must not run when the property is absent or false.

Do not add new dependencies for this.

## 4. Startup Hook

Use the simplest appropriate Spring startup hook already available, for example:

```text
ApplicationRunner
CommandLineRunner
```

Keep the implementation small.

The seeder should run only after the application context and persistence infrastructure are ready.

Do not implement custom lifecycle infrastructure.

## 5. Idempotency

The seeder must not insert duplicate demo data on every restart.

Use a simple rule:

```text
if no time deposits exist:
    insert demo data
else:
    do nothing
```

Do not use destructive cleanup.

Do not truncate existing data.

Do not overwrite user-created data.

Do not try to "synchronize" the database to the demo dataset.

## 6. Demo Dataset

Seed a small dataset that demonstrates important business boundaries.

Include representative time deposits such as:

```text
Basic    days=30
Basic    days=31
Student  days=365
Student  days=366
Premium  days=45
Premium  days=46
```

Choose clear balances that make resulting interest changes easy to inspect.

Include a small number of withdrawals for selected deposits so the GET endpoint demonstrates nested withdrawal data.

For example:

```text
2-3 withdrawals total
```

Do not create a huge dataset.

Do not add random data.

Use deterministic values.

## 7. Business Compatibility

The seeded values must respect the existing protected behavior.

Do not change:

- plan matching semantics
- day boundary semantics
- rounding behavior
- legacy `TimeDeposit`
- `TimeDepositCalculator.updateBalance`

The seeded dataset should intentionally make these existing rules visible to a reviewer.

## 8. Persistence Model Usage

Use the existing persistence adapter/entities/repositories appropriately.

Do not call the REST API from the seeder.

Do not invoke application use cases merely to insert demo rows.

Do not expose JPA entities outside the persistence adapter.

Keep seed construction local to the persistence adapter.

## 9. Transactions

Run the seed insertion in one small transaction if appropriate.

The goal is:

```text
either demo dataset is inserted successfully
or startup seed operation fails without leaving a strange partial dataset
```

Do not introduce distributed transaction mechanisms.

Do not add locking unless a concrete startup race exists.

## 10. Logging

Log seeding behavior concisely.

Examples:

```text
operation=demo_seed status=skipped reason=data_exists
```

```text
operation=demo_seed deposits=6 withdrawals=3 status=success
```

Do not log full entity contents.

Do not log sensitive configuration.

Keep this at INFO level.

## 11. Docker Compose

If the project includes `compose.yaml`, make the reviewer/demo path easy.

Prefer enabling seeding explicitly in Compose:

```yaml
environment:
  APP_DATABASE_SEED_ENABLED: "true"
```

Only do this if Compose is clearly intended as the local/demo startup path.

The base Spring configuration must remain disabled by default.

Do not hide the fact that Compose enables demo data.

## 12. Local Profile

If the project already uses a local/dev Spring profile, it is acceptable to enable seeding there.

Do not introduce a complicated profile hierarchy solely for this feature.

Prefer one clear mechanism.

Avoid simultaneously enabling seed in multiple overlapping places unless the behavior remains obvious.

## 13. Flyway Separation

Do not put demo data into the production Flyway migration history.

Flyway remains responsible for schema evolution.

Demo seed data remains optional runtime/demo initialization.

Do not add:

```text
Vx__insert_demo_data.sql
```

for this feature.

The distinction must remain clear:

```text
Flyway = schema
Seeder = optional demo data
```

## 14. Tests

Add focused tests for the seeding behavior.

Cover at minimum:

### Disabled

When:

```text
app.database.seed.enabled=false
```

verify no demo data is inserted.

### Enabled + Empty Database

When enabled and the database is empty:

- demo deposits are inserted
- expected withdrawal count is inserted
- representative plan/day values exist

### Enabled + Existing Data

When enabled and at least one time deposit already exists:

- seeder does not add the demo dataset
- existing data remains untouched

Use PostgreSQL/Testcontainers where practical.

Do not use H2.

Do not make tests depend on startup ordering accidentally.

## 15. Reviewer Experience

The final local/demo flow should be simple.

A reviewer should be able to:

```bash
docker compose up --build
```

then:

1. open Swagger UI
2. call `GET /time-deposits`
3. immediately see meaningful demo data
4. call the update-all balances endpoint
5. call GET again
6. observe balance changes for eligible plans

Document the exact verified commands/URLs in README.

Do not claim a URL unless verified against the actual implementation.

## 16. Security First

Keep default seeding disabled.

Do not seed:

- credentials
- API keys
- tokens
- real customer information
- realistic personally identifiable information

Use obviously synthetic demo data only.

Do not add dependencies unless required.

If build dependencies change, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

## 17. AGENTS.md

Add a concise rule for future agent sessions:

> Demo database seeding is optional infrastructure behavior, disabled by default, deterministic, idempotent, non-destructive, and must not be implemented as a production Flyway data migration.

Also state that seed data must remain synthetic and must not change domain behavior.

## 18. README.md

Add a concise **Demo Data** section.

Document:

- seeding is disabled by default
- property name:
  `app.database.seed.enabled`
- environment variable:
  `APP_DATABASE_SEED_ENABLED`
- whether Docker Compose enables it
- what representative cases are seeded
- how to disable it
- how the reviewer can verify GET -> update -> GET

Keep it concise.

## 19. Architecture Guidelines

Add a small guideline if useful:

```text
Optional environment/demo initialization belongs to infrastructure adapters,
not domain or application code.
```

Do not over-document.

## Verification

After implementation:

1. Run:

```bash
./mvnw clean test
```

2. Verify application startup with seeding disabled.

3. Confirm database remains empty if starting from an empty database.

4. Enable:

```text
APP_DATABASE_SEED_ENABLED=true
```

5. Start with an empty PostgreSQL database.

6. Confirm exactly the intended demo dataset is created.

7. Restart without clearing the database.

8. Confirm no duplicates are added.

9. Confirm GET returns the seeded data.

10. Call the update-all endpoint.

11. Call GET again.

12. Confirm expected balances change based on the existing rules.

13. Confirm withdrawals remain unchanged.

14. Verify INFO logs show concise seed status.

15. Confirm Flyway migration history contains no demo-data migration.

16. Confirm API contract, schema, and domain behavior remain unchanged.

17. If dependencies changed, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

## Constraints

- Default seeding must be disabled.
- No production Flyway demo-data migration.
- Seeder belongs to persistence/infrastructure.
- Seeder must be idempotent.
- Seeder must be non-destructive.
- Do not overwrite existing data.
- Use deterministic synthetic demo data.
- Keep dataset small.
- No new business endpoints.
- Do not change `docs/openapi.yaml`.
- Do not change required database schema names.
- Do not change legacy calculator behavior.
- Do not add dependencies unless genuinely required.
- Preserve Security First rules.

## Output

Before editing, provide:

1. current startup/configuration approach
2. proposed property location
3. proposed seeder package/class
4. exact deterministic demo dataset
5. idempotency strategy
6. Docker/local activation plan

After implementation, report:

1. Files changed
2. Property/configuration added
3. Seeder implementation
4. Demo deposits inserted
5. Demo withdrawals inserted
6. Idempotency behavior
7. Docker/local enablement
8. Logging behavior
9. Tests added
10. Full test result
11. Security scan result if dependencies changed
12. README/AGENTS/architecture documentation updates
13. Confirmation that Flyway contains schema changes only
14. Confirmation that seeding is disabled by default
15. Confirmation that API contract, schema, and business behavior were unchanged

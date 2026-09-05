# 12 - Utility Placement and Shared Code Cleanup

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- current package structure
- current domain code
- current application code
- current inbound and outbound adapters
- current helper/mapper/extension files
- current tests

Inspect the repository before making changes.

Assume the main architecture is already in place.

## Goal

Review helper, mapper, extension, conversion, formatting, and utility code and place each piece where it conceptually belongs in the hexagonal architecture.

The goal is to avoid a global `utils` package and prevent cross-layer coupling through generic helper classes.

Do not change business behavior.
Do not change API contracts.
Do not change persistence schema.
Do not perform unrelated refactoring.

## Core Rule

Do not create or keep a global catch-all package such as:

```text
utils/
util/
common/utils/
helpers/
```

unless there is a concrete, narrowly-scoped reason.

Use this rule:

> If a utility knows about the domain, it belongs to the domain. If it knows about a framework or external system, it belongs to the adapter. Only truly generic, dependency-free helpers belong to shared code.

## 1. Domain Helpers

If a function or class represents business meaning or business calculation, place it in the domain.

Examples:

```code
fun calculateInterest(...)
```

```kotlin
fun TimeDeposit.isEligibleForInterest(): Boolean
```

```code
fun BigDecimal.applyRate(...)
```

when the behavior is specifically part of the time-deposit business model.

Appropriate location:

```text
domain/
```

or a domain-specific package such as:

```text
domain/interest/
domain/timedeposit/
```

Do not hide domain logic inside `Utils`, `Common`, or adapter code.

## 2. Persistence-Specific Helpers

If a helper knows about:

- JPA
- persistence entities
- SQL/database representation
- database dates/types
- Spring Data
- persistence mapping

it belongs inside the outbound persistence adapter.

Examples:

```kotlin
fun TimeDepositEntity.toDomain()
fun TimeDepositModel.toEntity()
```

should live in something like:

```text
adapter/out/persistence/TimeDepositMapper.kt
```

A conversion such as:

```kotlin
fun LocalDate.toSqlDate()
```

belongs in the persistence adapter if it exists only to support database mapping.

Do not move persistence-specific mapping into shared code.

## 3. REST/API-Specific Helpers

If a mapper or helper knows about:

- REST DTOs
- HTTP
- API response models
- Spring MVC
- serialization concerns

keep it in the inbound REST adapter.

Examples:

```text
adapter/in/rest/TimeDepositResponseMapper.kt
```

Do not let REST DTO mapping leak into domain or application code.

## 4. Application-Specific Helpers

If a helper exists only to support application orchestration or use-case mapping, place it with the application layer.

Do not move it into `shared` merely because more than one application service calls it.

Shared code should not become a shortcut for avoiding ownership decisions.

## 5. Shared Code

Create or keep a `shared` package only for code that is genuinely:

- domain-agnostic
- framework-independent
- dependency-light
- reusable across multiple architectural areas
- stable enough to justify shared ownership

Good examples may include:

```text
shared/time/
shared/text/
```

but only when there is actual usage.

Examples of potentially acceptable shared helpers:

```kotlin
fun String.normalizeWhitespace(): String
```

A generic clock abstraction may also live in a shared/core package if it truly serves multiple independent areas and does not belong to a specific domain concept.

Do not create `shared` preemptively.

## 6. Avoid Generic Garbage Drawers

Avoid names such as:

- `Utils`
- `CommonUtils`
- `DateUtils`
- `MapperUtils`
- `StringUtils`
- `Helpers`
- `Common`
- `Misc`

Prefer names that reveal purpose and ownership.

Examples:

```text
TimeDepositMapper
WithdrawalMapper
InterestPolicy
CorrelationIdFilter
MoneyRounding
```

Only create abstractions that correspond to a concrete responsibility.

## 7. Extension Functions

Kotlin extension functions should live close to the type/context that owns their meaning.

Examples:

- domain-specific extension -> domain package
- persistence conversion -> persistence adapter
- REST conversion -> REST adapter
- truly generic text helper -> shared/text

Do not create a global `Extensions.kt` containing unrelated extensions.

Prefer focused files such as:

```text
TimeDepositMappings.kt
PersistenceDateMappings.kt
StringNormalization.kt
```

when extensions are actually useful.

## 8. Mapper Ownership

Keep mapping at architectural boundaries.

Examples:

```text
Persistence Entity -> Application/Domain Model
```

belongs to the persistence adapter.

```text
Application Read Model -> REST Response DTO
```

belongs to the REST adapter.

Do not create one global mapper that knows about:

- JPA entities
- domain objects
- application models
- REST DTOs

at the same time.

That creates cross-layer coupling and defeats the architecture.

## 9. Dependency Direction

After cleanup, verify that utility/helper code does not violate dependency direction.

Expected direction:

```text
adapter -> application/domain
application -> domain
domain -> nothing framework-specific
```

Shared code, if it exists, must not depend on adapters.

Domain code must not depend on:

- Spring
- JPA
- REST
- PostgreSQL
- Testcontainers

## 10. Keep Shared Small

If a helper is used only once or twice and its owner is obvious, keep it with that owner.

Do not extract code into `shared` simply to reduce duplication of one or two trivial lines.

Duplication may be cheaper than creating the wrong cross-layer abstraction.

## 11. Existing Legacy Classes

Do not move `TimeDeposit` or `TimeDepositCalculator` purely for package aesthetics if doing so could change their public package/API compatibility.

Evaluate first.

If moving either class can break hidden tests, external imports, or the protected contract, keep the existing package and treat it as a legacy compatibility boundary.

New domain logic should still follow the domain ownership rules.

## 12. Tests

Run the full suite after moving helper code.

Do not weaken characterization tests.

If helper relocation changes imports only, behavior must remain identical.

Add tests only if a helper contains meaningful logic that was previously untested.

Do not test trivial pass-through mapping excessively.

## 13. Documentation

Update:

- `AGENTS.md`
- `docs/architecture-guidelines.md`
- `README.md` only if useful

### AGENTS.md

Add a concise rule:

> Avoid global `utils` packages. Place helpers with the layer or concept that owns them. Only truly generic, dependency-free helpers belong in shared code.

Also document mapper ownership:

- persistence mapping stays in persistence adapter
- REST mapping stays in REST adapter
- business calculations stay in domain

### docs/architecture-guidelines.md

Add a concise **Utilities and Shared Code** guideline covering:

- no global utility garbage drawer
- ownership by concept/layer
- boundary-specific mapping
- minimal `shared`
- dependency direction

### README.md

Do not add a large utilities section.

Only update package structure documentation if the cleanup materially changes the documented architecture.

## Verification

After implementation:

1. Search the repository for packages/directories named:

```text
utils
util
helpers
common
```

2. Review every result and confirm whether it has a clear reason to exist.

3. Run:

```bash
./mvnw clean test
```

4. Confirm characterization tests remain green.

5. Confirm domain code has no framework dependencies.

6. Confirm persistence mappings remain inside the persistence adapter.

7. Confirm REST mappings remain inside the inbound REST adapter.

8. Confirm shared code, if any, is genuinely generic and dependency-free.

9. Confirm no API contract, database schema, or business behavior changed.

10. If dependencies/build configuration changed, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

## Constraints

- No global utility garbage drawer.
- Prefer ownership over generic reuse.
- Keep domain logic in domain.
- Keep persistence mapping in persistence adapter.
- Keep REST mapping in REST adapter.
- Keep application-specific helpers in application.
- Keep `shared` minimal.
- Do not move protected legacy classes purely for aesthetics.
- Do not change business behavior.
- Do not change `docs/openapi.yaml`.
- Do not change required database schema.
- Do not add dependencies for utility cleanup.
- Preserve Security First rules.

## Output

Before editing:

1. List all current helper/utility/mapper/extension files.
2. Classify each one by owner:
   - domain
   - application
   - inbound adapter
   - outbound adapter
   - truly shared
3. Propose the smallest cleanup plan.

After implementation, report:

1. Files moved
2. Files renamed
3. Global utility packages removed or retained
4. Domain helpers and their ownership
5. Persistence mappings and their ownership
6. REST mappings and their ownership
7. Shared helpers retained, with justification
8. Tests executed
9. Documentation updates
10. Confirmation that dependency direction remains valid
11. Confirmation that API, schema, and business behavior were unchanged

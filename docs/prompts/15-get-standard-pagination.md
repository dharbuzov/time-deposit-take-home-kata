# 15 - Standard Pagination for GET Time Deposits

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- current GET use case
- current REST adapter
- current persistence adapter
- current API tests

Inspect the repository before making changes.

## Goal

Add standard page-based pagination to the GET time-deposits endpoint.

Use the conventional API parameters:

```text
page
size
sort
```

Do not expose both `page` and raw `offset` at the same time.

`offset` should remain an internal database concept calculated from:

```text
offset = page * size
```

This step applies only to GET.

Do not modify the update-all balances endpoint.

## 1. Public API

Change:

```text
GET /time-deposits
```

to accept optional query parameters:

```text
?page=0&size=20&sort=id,asc
```

Use conventional zero-based page numbering.

Recommended defaults:

```text
page = 0
size = 20
sort = id,asc
```

Use a reasonable maximum page size, for example:

```text
max size = 100
```

Do not allow an unbounded page size.

## 2. Standard Parameters

### page

Zero-based page index.

Example:

```text
page=0
page=1
page=2
```

### size

Maximum number of deposits returned in one page.

Example:

```text
size=20
```

Enforce a reasonable upper bound.

### sort

Support simple Spring-style sorting:

```text
sort=id,asc
sort=id,desc
```

Only support fields that are safe and useful.

At minimum support:

```text
id
```

Do not expose arbitrary entity/property names blindly.

## 3. Offset

Do not expose a separate public `offset` parameter if `page` is already part of the API.

Internally:

```text
offset = page * size
```

Using both:

```text
page
offset
```

would create ambiguous request semantics.

Document this decision if useful.

## 4. Response Shape

Return a paginated response object rather than a raw array.

Prefer a small API-specific response such as:

```json
{
  "content": [
    {
      "id": 1,
      "planType": "basic",
      "balance": 1000.00,
      "days": 31,
      "withdrawals": []
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 125,
  "totalPages": 7
}
```

Keep framework-specific `Page` types out of the public contract.

Create an explicit API DTO.

Do not expose internal Spring/JPA pagination classes directly.

## 5. OpenAPI

Update `docs/openapi.yaml` before or together with implementation.

Document:

- `page`
- `size`
- `sort`
- defaults
- maximum size where appropriate
- paginated response schema

The OpenAPI document remains the API source of truth.

Do not modify the update-all endpoint.

## 6. Application Port

Keep pagination framework-independent.

Prefer an application request model such as:

```kotlin
data class PageRequest(
    val page: Int,
    val size: Int,
    val sort: SortSpec
)
```

and a result model such as:

```kotlin
data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
```

Names may differ to fit the current codebase.

Do not expose:

- Spring `Page`
- `Pageable`
- JPA-specific types

through application/domain ports.

## 7. Persistence Adapter

Spring Data pagination may be used inside the persistence adapter.

It is acceptable for the adapter to translate the application pagination request into:

```text
PageRequest
Sort
Page
```

internally.

Keep those framework types inside the adapter.

Avoid unnecessary custom SQL if standard Spring Data pagination is sufficient.

## 8. Withdrawals

Paginated deposits must still include withdrawals.

Inspect the current loading strategy and avoid an obvious N+1 query pattern.

For one page of deposits, do not execute one withdrawal query per deposit if a bounded fetch strategy can avoid it.

Use the smallest clean solution.

Do not change schema.

## 9. Validation

Handle pagination boundaries cleanly.

At minimum:

```text
page >= 0
size >= 1
size <= configured maximum
```

Keep validation small.

Do not build a large error-handling framework.

## 10. README Justification

Add a clear justification to `README.md`.

Use wording close to:

> The original requirement asks to retrieve all time deposits. Returning an unbounded dataset in a single response does not scale safely as the table grows, because it can cause excessive database reads, application memory usage, large JSON payloads, and long response times. The GET endpoint therefore uses standard page-based pagination (`page`, `size`, `sort`) with bounded page sizes. This is an intentional scalability trade-off that keeps the API predictable while preserving access to the complete dataset across pages.

Also explain:

> `offset` is not exposed separately because page-based pagination already derives it internally as `page * size`, and exposing both would create ambiguous semantics.

Keep this visible in an **Assumptions / Design Decisions** section.

This is an explicit deviation/interpretation of the literal "retrieve all" wording and must not be hidden.

## 11. Architecture Documentation

Update `docs/architecture-guidelines.md` with a concise pagination rule:

- public collection APIs should use bounded pagination for potentially large datasets
- page/size/sort are API concerns
- framework pagination types remain inside adapters
- application ports use framework-independent pagination models
- avoid unbounded `findAll()` at REST boundaries

Update `AGENTS.md` with the same principle.

## 12. Tests

Add/update API tests covering:

### Defaults

```text
GET /time-deposits
```

Verify:

- page 0
- default size
- default sorting

### Explicit page

```text
GET /time-deposits?page=1&size=2
```

Verify correct records.

### Sort

```text
GET /time-deposits?page=0&size=2&sort=id,desc
```

Verify ordering.

### Metadata

Verify:

- `page`
- `size`
- `totalElements`
- `totalPages`

### Boundaries

Verify:

- page 0 works
- empty page works
- invalid negative page is rejected
- zero/negative size is rejected
- excessive size is rejected or capped according to the chosen contract

### Withdrawals

Verify nested withdrawals remain correct.

Use PostgreSQL/Testcontainers for meaningful integration coverage.

## 13. Update-All Must Remain Untouched

Do not change:

```text
POST /time-deposits/balances
```

Do not change:

- transaction semantics
- batching
- calculation
- persistence behavior
- tests

This prompt is GET pagination only.

## 14. Security First

Do not allow arbitrary uncontrolled sorting expressions to reach persistence.

Whitelist supported sort fields.

Do not add dependencies unless required.

If dependencies change, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

## Verification

After implementation:

1. Run:

```bash
./mvnw clean test
```

2. Confirm GET supports `page`, `size`, and `sort`.

3. Confirm zero-based page numbering.

4. Confirm page size is bounded.

5. Confirm `offset` is not exposed publicly.

6. Confirm pagination metadata is returned.

7. Confirm withdrawals remain correct.

8. Confirm no obvious N+1 regression.

9. Confirm OpenAPI matches implementation.

10. Confirm README contains the scalability justification.

11. Confirm the deviation from literal "retrieve all" wording is explicitly documented.

12. Confirm update-all remained unchanged.

13. If dependencies changed, run dependency/security checks.

## Constraints

- GET-only change.
- Standard page-based pagination.
- Public parameters: `page`, `size`, `sort`.
- Do not expose raw `offset` together with `page`.
- Bounded page size.
- Explicit API pagination DTOs.
- No Spring pagination types outside adapters.
- Update `docs/openapi.yaml`.
- Add explicit README justification.
- Avoid N+1 queries.
- Do not modify update-all.
- Preserve schema and business calculation behavior.
- Preserve Security First rules.

## Output

Before editing, report:

1. Current GET contract
2. Current response shape
3. Current persistence access
4. Proposed defaults for `page`, `size`, `sort`
5. Proposed maximum size
6. Proposed response schema
7. Proposed README justification
8. Confirmation that update-all will remain untouched

After implementation, report:

1. Files changed
2. OpenAPI changes
3. Pagination request model
4. Pagination response model
5. Adapter mapping to Spring Data pagination
6. Sort whitelist
7. Withdrawal loading strategy
8. Tests added/updated
9. Full test result
10. README justification added
11. AGENTS/architecture documentation updates
12. Confirmation that update-all, schema, and interest behavior remained unchanged

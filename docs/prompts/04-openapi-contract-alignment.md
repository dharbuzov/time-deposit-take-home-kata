# 04 - OpenAPI Contract Alignment

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- `docs/erd.puml`

Inspect the current repository before making changes.

Assume infrastructure baseline is already in place.

## Goal

Align the OpenAPI contract with the assignment requirements and the actual documented persistence model before implementing REST controllers.

This step is contract-only.

Do not implement controllers.
Do not implement business use cases.
Do not refactor `TimeDepositCalculator`.
Do not change persistence schema unless required to match the README.

## Source of Truth

Use this priority when resolving conflicts:

1. Assignment requirements documented in `README.md`
2. Existing protected legacy behavior where relevant
3. `docs/openapi.yaml`
4. `docs/erd.puml`

If OpenAPI or ERD conflicts with the README, update the documentation artifact rather than changing the assignment requirements.

## Required API Surface

The application must expose exactly two business REST endpoints:

### 1. Retrieve all time deposits

Return all time deposits.

The response model must expose:

- `id`
- `planType`
- `balance`
- `days`
- `withdrawals`

Each withdrawal should expose only fields required by the assignment/documentation.

Do not add:

- filtering
- pagination
- sorting parameters
- search
- additional CRUD operations
- extra business endpoints

unless explicitly required by the assignment.

### 2. Update balances of all time deposits

Define one endpoint that triggers balance update for all persisted time deposits.

Keep the contract simple.

Do not introduce request payloads unless the assignment requires one.

Use a success response with no unnecessary response body if the operation does not need to return data.

## Money Representation

For monetary values such as:

- `balance`
- withdrawal `amount`

use JSON numeric values.

In OpenAPI:

```yaml
type: number
```

Do not use:

```yaml
format: double
```

Do not change monetary fields to strings merely to represent `BigDecimal`.

The implementation may use `BigDecimal` internally for new persistence/API monetary code while preserving the legacy `TimeDeposit` contract.

## Plan Type

Do not silently change the legacy `TimeDeposit.planType` contract.

The legacy calculator currently works with string values.

If the OpenAPI document contains examples, make them consistent with actual behavior and documented assumptions.

Do not introduce an enum if it would create unnecessary coupling or break compatibility.

If an enum is used only at the API boundary, explicitly document how it maps to the legacy string representation before implementing it.

Prefer the smallest compatible contract.

## Withdrawals

The GET response must contain withdrawals for each time deposit.

Do not modify the shared legacy `TimeDeposit` class merely to add withdrawals.

Treat the REST response as a dedicated API/read DTO.

Do not create additional withdrawal endpoints.

## HTTP Semantics

Review and document:

- paths
- HTTP methods
- response status codes
- content types
- response schemas
- operation IDs
- required fields

Keep semantics conventional and minimal.

For the balance-update endpoint, prefer a successful no-content response when no response body is required.

Do not invent asynchronous job semantics.

## Contract Quality

Ensure `docs/openapi.yaml`:

- is valid OpenAPI
- contains exactly the two required business paths
- has reusable component schemas where useful
- clearly marks required fields
- contains no unused schemas
- contains no speculative API features
- is readable without generated noise

Keep the document small.

## Documentation Consistency

After updating `docs/openapi.yaml`, verify consistency with:

- `README.md`
- `docs/architecture-guidelines.md`
- `docs/erd.puml`

Do not duplicate the full contract into README.

README should point to `docs/openapi.yaml` as the API contract.

If any documentation still references `docs/openapi.yml`, update it to:

`docs/openapi.yaml`

## Verification

After editing:

1. Validate `docs/openapi.yaml` syntax.
2. Confirm there are exactly two business paths.
3. Confirm GET returns:
   - `id`
   - `planType`
   - `balance`
   - `days`
   - `withdrawals`
4. Confirm money fields do not use `format: double`.
5. Confirm no filters, pagination, CRUD, or extra endpoints were added.
6. Confirm documentation references use `docs/openapi.yaml`.
7. Confirm no production Kotlin code was changed.

## Constraints

- Contract-only step.
- Do not implement controllers.
- Do not implement services/use cases.
- Do not implement persistence changes.
- Do not modify the required database table or column names.
- Do not change `TimeDeposit`.
- Do not change `TimeDepositCalculator.updateBalance`.
- Do not change legacy calculator behavior.
- Exactly two business REST endpoints.
- No speculative API functionality.
- Prefer the smallest clear contract.

## Output

Before editing, provide a short plan.

After editing, report:

1. Files changed
2. Endpoint paths and methods
3. Response status codes
4. Final response schemas
5. Money representation
6. Any assumptions made
7. Validation performed
8. Confirmation that no production code was changed

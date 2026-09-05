# 10 - REST Adapters and API Implementation

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- current application use cases
- current API/read models
- current persistence adapter
- current tests

Inspect the repository before making changes.

Assume:

- characterization tests are complete
- infrastructure baseline is complete
- OpenAPI contract is aligned
- security baseline is complete
- hexagonal architecture skeleton is complete
- persistence adapter is complete
- interest refactoring is complete
- application use cases and transaction boundary are complete

## Goal

Implement the two REST endpoints defined in `docs/openapi.yaml` as thin inbound adapters over the existing application use cases.

The OpenAPI contract is already agreed and is the API source of truth.

Do not add new business functionality.
Do not change the database schema.
Do not change interest behavior.
Do not add extra endpoints.

## 1. Implement Exactly Two Business Endpoints

Implement exactly the two paths already defined in:

`docs/openapi.yaml`

Do not create additional CRUD endpoints.

Do not add:

- create deposit
- update deposit
- delete deposit
- get deposit by id
- withdrawal endpoints
- filters
- pagination
- sorting
- search
- health/business utility endpoints beyond framework/infrastructure defaults

unless explicitly required by the assignment.

## 2. GET Time Deposits

Implement the GET endpoint for retrieving all persisted time deposits.

The controller must delegate to the existing get-all use case.

The response must match the OpenAPI contract exactly.

Each time deposit must expose:

- `id`
- `planType`
- `balance`
- `days`
- `withdrawals`

Each withdrawal must expose exactly the fields required by `docs/openapi.yaml`.

Do not expose JPA entities directly.

Do not return the protected legacy `TimeDeposit` object directly if it cannot represent the contract cleanly.

Use the dedicated API response DTO/read mapping.

## 3. Update All Balances Endpoint

Implement the balance-update endpoint exactly as defined in the OpenAPI contract.

The controller should:

1. receive the HTTP request
2. invoke the existing update-all application use case
3. return the contract-defined success status

Do not duplicate interest calculation or transaction logic in the controller.

Do not load repositories directly from the controller.

Do not introduce a request body unless the OpenAPI contract requires one.

Do not return unnecessary data if the contract defines a no-content response.

## 4. Thin Controller Rule

REST controllers are adapters only.

They may contain:

- HTTP annotations
- request/response DTO mapping
- use-case invocation
- status/response construction

They must not contain:

- interest rules
- persistence logic
- JPA repository access
- transaction orchestration
- locking logic
- domain policy selection

Keep controllers small and obvious.

## 5. OpenAPI Contract Fidelity

Treat `docs/openapi.yaml` as the source of truth for:

- paths
- HTTP methods
- response codes
- content types
- schemas
- required fields
- field names

Do not silently change the contract to fit implementation convenience.

If implementation reveals a genuine contradiction in the contract, stop and report it before changing `docs/openapi.yaml`.

Do not generate extra API behavior outside the contract.

## 6. Money Representation

The API must expose monetary values as JSON numbers.

New API DTO monetary fields should use `BigDecimal` where appropriate.

Do not expose persistence `Double` values from new code.

Do not change the legacy `TimeDeposit.balance` type.

Preserve the existing compatibility conversion only where required by the application/calculator boundary.

## 7. Plan Type

Preserve the current contract and legacy compatibility for plan type values.

Do not introduce case normalization in the REST layer unless the contract explicitly requires it.

Do not add an enum solely for aesthetics if it risks changing accepted/returned values.

Return the persisted/application plan type representation required by the contract.

## 8. Withdrawals

Map withdrawals into the GET response.

Do not modify the legacy `TimeDeposit` class to carry withdrawals.

Do not expose persistence entities directly.

Keep withdrawal mapping explicit and small.

## 9. Error Handling

The assignment does not require elaborate invalid-input or exception handling.

Keep error handling minimal and conventional.

Do not build a large global error framework.

Do not expose:

- stack traces
- SQL errors
- internal class names
- credentials
- infrastructure details

through HTTP responses.

If a small global exception mapping is already required by Spring defaults or current architecture, keep it minimal.

Do not invent new business error semantics.

## 10. Swagger / OpenAPI Usability

Ensure the application can be exercised against the documented OpenAPI contract.

If the project already includes Swagger UI / springdoc support, verify it works.

If Swagger UI is not yet available and the assignment explicitly expects triggering endpoints via Swagger, add the smallest compatible dependency/configuration required.

Do not introduce code generation unless already justified.

Document the actual Swagger UI location in README only after verifying it.

If adding a dependency, follow the Security First dependency workflow.

## 11. API Tests

Add focused API integration tests.

Use Spring Boot test support and the existing PostgreSQL/Testcontainers setup where appropriate.

Cover at minimum:

### GET

- returns HTTP 200
- returns empty array when no deposits exist
- returns multiple deposits
- response field names match the contract
- withdrawals are included correctly
- monetary values are serialized as numbers

### Update balances

- returns the contract-defined success status
- updates persisted balances through the application use case
- does not modify withdrawals
- GET after update reflects persisted balance changes

Prefer tests through the HTTP boundary for contract behavior.

Do not duplicate all interest-boundary unit tests here.

## 12. Contract Assertions

Where practical, add tests that guard the important public contract details:

- exact endpoint paths
- exact HTTP methods
- exact status codes
- required response fields
- exactly the intended business endpoints

Avoid fragile tests that merely mirror implementation annotations.

Focus on observable HTTP behavior.

## 13. Transaction Ownership

The controller must not define the business transaction boundary.

The update-all transaction remains owned by the application use case implemented in the previous step.

Do not add `@Transactional` to the controller for convenience.

## 14. Security First

Follow the repository Security First rules.

At the HTTP boundary:

- do not leak internal exceptions
- do not log secrets
- validate untrusted input when applicable
- keep exposed surface minimal
- do not add unnecessary dependencies

If Swagger/springdoc or any dependency is added or changed, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

Then run the full test suite.

## 15. Documentation

Update `README.md` to reflect the API that now actually exists.

Include:

- how to start the application
- how to access Swagger UI, if available
- how to invoke the two endpoints
- expected success status for update-all
- where the OpenAPI contract lives

Keep examples concise.

Do not duplicate the full OpenAPI document in README.

Update `docs/architecture-guidelines.md` only if necessary to state that REST controllers are thin inbound adapters.

Do not change `docs/openapi.yaml` unless a genuine contract defect is discovered and explicitly reported first.

## Verification

After implementation:

1. Run:

```bash
./mvnw clean test
```

2. Confirm characterization tests still pass.

3. Confirm persistence tests still pass.

4. Confirm application transaction/rollback tests still pass.

5. Confirm API integration tests pass.

6. Start the application if practical and verify the two endpoints.

7. Verify Swagger UI if included.

8. Confirm GET response matches `docs/openapi.yaml`.

9. Confirm update endpoint returns the documented status.

10. Confirm no extra business endpoint was added.

11. Confirm controller code contains no business or persistence logic.

12. Confirm database schema and legacy behavior were unchanged.

13. If dependencies changed, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

## Constraints

- Exactly two business REST endpoints.
- `docs/openapi.yaml` is the API source of truth.
- Keep controllers thin.
- Controllers depend on inbound use cases, not repositories.
- Do not change `TimeDeposit`.
- Do not change `TimeDepositCalculator.updateBalance` signature.
- Do not change characterized interest behavior.
- Do not change required database table or column names.
- Do not add filters, pagination, search, or CRUD endpoints.
- Do not add speculative error-handling frameworks.
- Do not move transaction ownership into controllers.
- Use dedicated API DTOs.
- Keep new monetary API code on `BigDecimal`.
- Preserve Security First rules.

## Output

Before editing, provide a short REST implementation plan and list the exact two endpoint methods/paths from `docs/openapi.yaml`.

After implementation, report:

1. Files changed
2. Controllers added
3. Exact endpoint paths and methods
4. Response DTOs/mapping used
5. Status codes implemented
6. Swagger/OpenAPI UI setup, if any
7. API tests added
8. Full test results
9. Security scan results if dependencies changed
10. README/documentation updates
11. Confirmation that exactly two business endpoints exist
12. Confirmation that controllers contain no persistence/business logic
13. Confirmation that OpenAPI contract, schema, and legacy behavior were preserved

# 11 - Logging and Observability Baseline

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- current REST adapters
- current application use cases
- current Spring Boot configuration
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
- application use cases are complete
- REST adapters are complete

## Goal

Add a small, production-minded logging and observability baseline using the logging stack already provided by Spring Boot.

The goal is useful operational visibility without introducing a full observability platform.

Do not add ELK, OpenSearch, Loki, Prometheus, Grafana, tracing backends, agents, or other infrastructure.

Do not change API behavior.
Do not change persistence schema.
Do not change business rules.

## 1. Use Existing Spring Logging Stack

Use the logging stack already available through Spring Boot:

- SLF4J
- Logback

Do not add another logging framework.

Do not add a logging dependency unless the current Spring Boot setup is missing something genuinely required.

Prefer structured and consistent log messages.

## 2. Logging Principles

Use log levels intentionally.

### INFO

Use for important application/business operations such as:

- start/completion of update-all balance operation
- number of processed time deposits
- execution duration
- successful application startup where useful

Do not log every object or every row.

### DEBUG

Use only for technical diagnostic details that are useful during development.

Do not enable verbose framework logging globally.

### WARN

Use for recoverable or suspicious conditions where appropriate.

### ERROR

Use for failed application operations that require investigation.

Include enough context to diagnose the operation without exposing sensitive information.

## 3. Update-All Use Case Logging

Add useful logging around the update-all balances application operation.

At minimum capture:

- operation name
- number of processed time deposits
- duration
- success/failure outcome

A conceptual format:

```text
operation=update_balances deposits=42 durationMs=87 status=success
```

On failure, log the operation context and error type.

Do not log:

- full time-deposit objects
- balances for every account/deposit
- withdrawal history
- database credentials
- connection strings
- secrets
- tokens

Keep the log volume proportional to the operation.

## 4. GET Endpoint Logging

Do not add noisy per-request/per-record business logs for normal GET operations.

If request logging is already handled at the HTTP boundary, avoid duplicating it inside the application service.

A single DEBUG-level summary is acceptable if it provides real diagnostic value.

Do not log every returned time deposit or withdrawal.

## 5. Correlation ID

Add a lightweight HTTP correlation ID mechanism.

Use request header:

```text
X-Correlation-ID
```

Behavior:

1. If the incoming request contains a non-blank `X-Correlation-ID`, reuse it.
2. Otherwise generate a UUID.
3. Put the correlation ID into SLF4J MDC for the lifetime of the request.
4. Return the same correlation ID in the HTTP response header.
5. Always clear the MDC value after request processing, including failure paths.

Keep the implementation small, for example with a servlet filter.

Do not add a distributed tracing framework.

## 6. Log Pattern

Update Logback/Spring logging configuration so the correlation ID is visible in normal application logs.

Prefer a concise pattern that includes:

- timestamp
- level
- logger/class
- correlation ID
- message

Do not create a complex custom logging format unless justified.

If JSON logging would require an additional dependency, do not add it solely for this assignment.

Readable structured key-value messages are sufficient.

## 7. Error Logging

When an application operation fails:

- log the failure once at the appropriate ownership boundary
- include operation context
- preserve the original exception
- avoid duplicate ERROR logs at multiple layers

Do not expose internal exception details through REST responses.

Do not log secrets or sensitive configuration values.

Avoid catch-log-rethrow patterns at every layer.

Prefer logging where the failure has meaningful operational context.

## 8. Sensitive Data and Security

Follow Security First rules.

Never log:

- passwords
- credentials
- tokens
- private keys
- database URLs containing credentials
- environment secrets

Avoid logging complete domain/persistence objects where they may contain business-sensitive values.

Do not log request/response bodies by default.

Do not enable Hibernate SQL parameter logging in normal configuration.

## 9. Metrics Without a Metrics Platform

Do not add Micrometer/Prometheus infrastructure solely for this step unless it already exists through Spring Boot and can be used without new external setup.

Operationally useful information for this assignment can be captured through concise logs:

- processed item count
- operation duration
- status

Do not invent dashboards or metric infrastructure.

## 10. Tests

Add focused tests only where they provide meaningful protection.

At minimum test correlation ID behavior:

- incoming `X-Correlation-ID` is returned
- missing correlation ID is generated
- correlation ID is available during request processing
- MDC is cleared after request completion if practical to verify

Avoid brittle tests that assert complete formatted log strings.

Do not create tests for every INFO message.

If logging around update-all can be tested without fragile implementation coupling, keep the test minimal.

## 11. Configuration

Keep logging configuration environment-friendly.

Avoid:

- hardcoded local filesystem log paths
- environment-specific absolute paths
- huge retained log files
- custom appenders without need

Console logging is sufficient for the take-home.

Do not add external logging infrastructure.

## 12. AGENTS.md

Add or update a concise **Logging and Observability** section in `AGENTS.md`.

Future agent sessions must follow these rules:

- use SLF4J/Logback already provided by Spring Boot
- log business operation outcomes, not internal noise
- include correlation ID for HTTP request context
- never log secrets or credentials
- avoid logging full domain/persistence objects
- avoid duplicate exception logging across layers
- do not add observability infrastructure without a concrete requirement
- keep logs useful at INFO and diagnostic details at DEBUG
- preserve Security First rules

## 13. README.md

Add a concise **Logging and Observability** section.

Document:

- SLF4J/Logback
- `X-Correlation-ID`
- generated correlation ID behavior
- correlation ID response header
- update-all operation summary logging
- console logging
- no external observability stack required

Add the section to the README Table of Contents if appropriate.

Do not over-document implementation details.

## 14. docs/architecture-guidelines.md

Add a concise **Logging and Observability** section as a cross-cutting concern.

Cover:

- correlation IDs
- operational event logging
- log levels
- sensitive-data avoidance
- exception logging ownership
- no speculative observability infrastructure

Reinforce:

> Observability should make failures diagnosable without making the system noisy or exposing sensitive data.

## Verification

After implementation:

1. Run:

```bash
./mvnw clean test
```

2. Start the application if practical.

3. Call the GET endpoint with:

```text
X-Correlation-ID: test-correlation-id
```

Confirm the same value is returned in the response.

4. Call the endpoint without the header.

Confirm a correlation ID is generated and returned.

5. Verify application logs include the correlation ID during request processing.

6. Trigger the update-all endpoint.

Confirm one concise operation summary is logged with:

- operation
- processed count
- duration
- status

7. Verify sensitive values are not logged.

8. Confirm no API contract, database schema, or business behavior changed.

9. If build/dependency configuration changed, run:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
```

## Constraints

- Use existing SLF4J + Logback.
- Do not add ELK/OpenSearch/Loki.
- Do not add Prometheus/Grafana.
- Do not add distributed tracing.
- Do not log secrets.
- Do not log full persistence/domain objects.
- Do not enable noisy SQL parameter logging.
- Do not change `docs/openapi.yaml`.
- Do not change database schema.
- Do not change legacy calculator behavior.
- Keep correlation ID implementation lightweight.
- Keep logging volume low and operationally useful.
- Preserve Security First rules.

## Output

Before editing, provide a short logging/observability plan.

After implementation, report:

1. Files changed
2. Logging configuration changes
3. Correlation ID implementation
4. MDC lifecycle handling
5. Update-all operation logging
6. Error logging approach
7. Tests added
8. Full test results
9. Security scan results if dependencies changed
10. `AGENTS.md` updates
11. `README.md` updates
12. `docs/architecture-guidelines.md` updates
13. Confirmation that no sensitive data is intentionally logged
14. Confirmation that API contract, schema, and business behavior were unchanged

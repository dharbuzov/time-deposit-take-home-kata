# 05 - Security Baseline

Read first:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- `docs/openapi.yaml`
- current `pom.xml`

Inspect the current dependency tree and build configuration before making changes.

Assume:

- infrastructure baseline is already implemented
- OpenAPI contract alignment is already complete

## Goal

Add a practical, reproducible Maven-based security baseline for dependency vulnerability scanning and dependency hygiene.

This is a security/build step.

Do not implement new business functionality.
Do not refactor domain logic.
Do not change API contracts.
Do not change persistence schema.

## 1. OWASP Dependency-Check

Add OWASP Dependency-Check to the existing Maven build.

Use:

- `org.owasp:dependency-check-maven`

The scan must be runnable through Maven Wrapper:

```bash
./mvnw dependency-check:check
```

Configure it minimally and clearly.

Do not add unrelated security plugins.

## 2. Dependency Tree

Use Maven dependency analysis to understand where vulnerable dependencies come from.

Run:

```bash
./mvnw dependency:tree
```

For relevant findings, classify them as:

- direct production dependency
- transitive production dependency
- test-only dependency
- build/plugin dependency

For every High or Critical finding, identify:

- affected component
- CVE/advisory if available
- severity
- direct or transitive origin
- currently resolved version
- fixed version if available
- whether the vulnerable code path is relevant
- proposed action

## 3. Fix Strategy

Prefer fixes in this order:

1. Use the Spring Boot BOM / managed dependency set.
2. Upgrade Spring Boot to a safe compatible patch/minor release when that resolves the issue.
3. Upgrade a direct dependency when this project owns that version.
4. Override a transitive dependency only when necessary and compatibility is understood.
5. Suppress a finding only when it is confirmed to be false-positive or non-applicable.

Avoid:

- random dependency overrides
- broad dependency modernization
- unnecessary major upgrades
- upgrading Java merely for unrelated security findings
- replacing libraries without a concrete reason
- changing architecture to eliminate a scanner warning

Preserve the existing Java 17 baseline unless a required security fix makes that technically impossible.

## 4. Spring Boot Dependency Management

Treat Spring Boot dependency management as the default source for compatible dependency versions.

Do not manually pin transitive dependency versions when the Spring Boot BOM already manages them unless there is a concrete security reason.

If a manual override is required:

- explain why
- identify the CVE being addressed
- verify compatibility
- keep the override narrow

## 5. False Positives and Suppressions

OWASP Dependency-Check may report false positives.

If a suppression is truly required, use a dedicated file such as:

```text
dependency-check-suppressions.xml
```

Rules:

- do not suppress findings just to make the build green
- keep suppressions narrow
- document the affected CVE/component
- include a short technical justification
- remove obsolete suppressions when no longer needed

## 6. Severity Policy

Review all findings.

Critical and High vulnerabilities require either:

- a compatible fix
- or explicit documented justification for why they remain

Medium and Low findings should still be reviewed, but do not introduce risky or disproportionate changes solely to eliminate them.

Do not configure an arbitrary failure threshold before reviewing the actual project findings.

If a failure threshold is introduced, document the reason.

## 7. Regression Safety

Security fixes must preserve existing behavior.

After dependency or build changes, run:

```bash
./mvnw clean test
```

Characterization tests must continue to pass.

Also verify existing infrastructure tests, including:

- Spring application context/bootstrap
- PostgreSQL Testcontainers integration
- Flyway migration startup

Do not change business behavior to satisfy security tooling.

## 8. Security First in Repository Guidance

Security is a first-class engineering concern for this repository.

Update all three documents:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`

Document only practices and tools that actually exist.

### AGENTS.md

Add a concise **Security First** section to the persistent Codex instructions.

Future agent sessions must follow these rules:

- never commit credentials, tokens, secrets, or private keys
- do not hardcode production credentials
- minimize new dependencies
- inspect dependency impact when adding or upgrading libraries
- prefer Spring Boot/BOM-managed dependency versions
- review High and Critical vulnerability findings explicitly
- do not suppress findings without documented justification
- run the Maven security scan after dependency or build changes
- preserve characterized business behavior during security fixes
- validate untrusted input at system boundaries where applicable
- use safe/parameterized persistence access
- do not expose secrets or unnecessary internal exception details through APIs or logs
- use least privilege where practical
- do not introduce heavyweight security infrastructure without a concrete requirement

Include the actual repository commands:

```bash
./mvnw dependency:tree
./mvnw dependency-check:check
./mvnw clean test
```

Make it clear that dependency/build changes trigger the dependency security checks.

### README.md

Add a concise **Security** section and include it in the Table of Contents.

Document:

- Security First principle
- OWASP Dependency-Check
- Maven Wrapper scan command
- Maven dependency tree command
- generated report location
- how High/Critical findings are handled
- suppression policy
- environment-based credentials/configuration
- no committed secrets

Keep commands copy-pasteable and consistent with the actual repository.

### docs/architecture-guidelines.md

Add a concise dedicated **Security** section.

Cover:

- dependency vulnerability management
- minimal dependency surface
- secrets/configuration hygiene
- trust-boundary input validation
- safe persistence access
- safe logging/error handling
- least privilege where practical
- security review for dependency, infrastructure, API-boundary, and persistence changes

Reinforce this principle:

> Security first, but complexity must be justified.

Do not add speculative enterprise security architecture such as:

- service mesh
- WAF
- Vault
- SIEM
- external IAM platforms
- dedicated security infrastructure

unless explicitly required.

## 9. Generated Reports

Do not commit large generated vulnerability reports.

OWASP Dependency-Check output under build directories such as `target/` should remain outside source control.

Update `.gitignore` only if required.

## Verification Workflow

Run:

```bash
./mvnw dependency:tree
```

Then:

```bash
./mvnw dependency-check:check
```

Review the findings.

Apply only justified compatible fixes.

Then run:

```bash
./mvnw clean test
./mvnw dependency-check:check
```

Verify:

- characterization tests still pass
- infrastructure tests still pass
- no business behavior changed
- no API contract changed
- no persistence schema changed
- High/Critical findings are fixed or explicitly justified

## Constraints

- Keep Maven and Maven Wrapper.
- Use Maven-based security tooling only for this step.
- Do not add Trivy in this step.
- Preserve Java 17 unless a concrete required security fix prevents it.
- Do not change `TimeDeposit`.
- Do not change `TimeDepositCalculator.updateBalance` signature.
- Do not change legacy calculator behavior.
- Do not change the two-endpoint OpenAPI contract.
- Do not change required database table or column names.
- Do not add unrelated infrastructure.
- Do not perform broad dependency modernization.
- Prefer minimal compatible security fixes.
- Never suppress findings without documented justification.

## Output

Before editing, provide a short plan.

After implementation, report:

1. Files changed
2. OWASP Dependency-Check configuration
3. Dependency tree findings
4. Vulnerabilities found
5. Classification of High/Critical findings
6. Dependency changes made
7. Suppressions added, if any, with reasons
8. Test results
9. Dependency-Check result after fixes
10. Documentation changes in `AGENTS.md`
11. Documentation changes in `README.md`
12. Documentation changes in `docs/architecture-guidelines.md`
13. Remaining vulnerabilities and justification
14. Confirmation that API, schema, and business behavior were unchanged

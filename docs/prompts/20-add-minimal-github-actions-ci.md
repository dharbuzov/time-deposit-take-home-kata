# 20 - Add Minimal GitHub Actions CI

Read the final repository first: `AGENTS.md`, `README.md`, `kotlin/pom.xml`, Maven Wrapper, tests/Testcontainers configuration, `.gitignore`, and any existing `.github/` files.

## Goal

Add one minimal GitHub Actions workflow proving that a clean checkout can run the same build/test path documented for the reviewer.

Do not add application features or turn the take-home into a large delivery platform.

## Scope

Create, unless an equivalent workflow already exists:

```text
.github/
└── workflows/
    └── ci.yml
```

If CI already exists, inspect and fix/simplify it rather than duplicating it.

Run on:

```yaml
on:
  push:
  pull_request:
```

Use:

- `ubuntu-latest`
- Java 17, matching the project baseline
- Eclipse Temurin unless the repo already justifies another distribution
- the committed Maven Wrapper
- Maven dependency caching through the Java setup action where appropriate

Use current supported stable major versions of official GitHub actions. Verify versions rather than copying stale examples.

The CI command must be:

```bash
cd kotlin
./mvnw clean test
```

or equivalent through `defaults.run.working-directory: kotlin`.

Do not install global Maven.

## Testcontainers

The suite uses PostgreSQL Testcontainers.

Verify that the GitHub-hosted runner can execute the existing Testcontainers tests using its Docker runtime.

Prefer Testcontainers owning PostgreSQL lifecycle. Do NOT add a separate PostgreSQL Actions service if Testcontainers already manages it.

Do not introduce H2.

Report whether PostgreSQL/Testcontainers tests are expected to execute in CI rather than silently assuming they do.

## Keep CI proportional

Do NOT add:

- OWASP Dependency-Check to the required PR test job unless already proven reliable
- Trivy
- Snyk
- Sonar
- CodeQL
- Dependabot
- release/deployment workflows
- semantic-release
- changelog automation
- `ISSUE_TEMPLATE`
- `PULL_REQUEST_TEMPLATE.md`
- `CODEOWNERS`
- branch-protection documentation

The repository already documents security verification separately. Avoid making normal PR CI dependent on external NVD/network availability.

## Permissions

Use least privilege for this test-only workflow where practical, for example read-only repository contents.

Do not request deployment/write permissions or require secrets.

## README

Do not create a large CI section.

At most add one short sentence that GitHub Actions runs the Maven test suite.

Only add a badge if the real repository owner/name is known and the badge can be correct. Never invent a GitHub URL.

## Verification

Before finishing verify:

1. YAML syntax is valid.
2. Workflow is under `.github/workflows/`.
3. Java 17 is configured.
4. Maven Wrapper is used.
5. Working directory correctly targets `kotlin/`.
6. No global Maven installation.
7. No duplicate PostgreSQL service alongside Testcontainers.
8. No H2.
9. No secrets required.
10. No unnecessary write/deployment permissions.
11. No issue/PR templates added.
12. No release/deployment workflow added.
13. Local documented command remains:

```bash
cd kotlin
./mvnw clean test
```

14. If practical, execute the same test command locally and report the actual result.

## Git

Do not commit automatically unless explicitly requested.

Suggested atomic commit message:

```text
ci: add GitHub Actions test workflow
```

Do not rewrite history.

## Output

Before editing report:

1. whether `.github/` exists
2. existing workflows/templates
3. Java version from Maven
4. Maven Wrapper status
5. Testcontainers/Docker behavior
6. proposed workflow

After implementation report:

1. files changed
2. triggers
3. runner/JDK
4. exact Maven command
5. Testcontainers behavior
6. permissions
7. whether README changed
8. local test result if executed
9. confirmation that no templates/release/deployment/unrelated CI tooling were added
10. suggested atomic commit message

The final result should be one small CI workflow proving the solution builds and tests from a clean checkout.

# 19 - Document the AI Engineering Harness

Read the original assignment AI requirement, `AGENTS.md`, all `docs/prompts/`, `docs/architecture-guidelines.md`, `docs/openapi.yaml`, `docs/erd.puml`, tests, Maven/Docker/security verification setup, and final `README.md`.

## Goal

Create `docs/ai-assisted-development.md` documenting the actual, practical and reproducible AI-assisted engineering approach used for this take-home.

Do not modify application behavior.

## Core model

Document the repository itself as the harness:

```text
Repository knowledge / constraints
              ↓
        Focused task prompt
              ↓
             Codex
              ↓
       Proposed change
              ↓
   Tests + runtime verification
              ↓
         Reviewer pass
```

Core principle:

```text
AI proposes
repository constraints guide
tests/contracts verify
engineer owns the result
```

State factually that OpenAI Codex was used as the engineering/coding agent. Do not invent model names, token counts or hidden configuration.

## Repository as the harness

Explain actual responsibilities:

```text
AGENTS.md
-> persistent rules and navigation

docs/architecture-guidelines.md
-> architecture and engineering decisions

docs/openapi.yaml
-> public API contract

docs/erd.puml
-> persistence model

tests
-> executable behavioral constraints

docs/prompts/
-> reproducible focused task workflow/history
```

Explain that durable project constraints live in version-controlled files instead of being repeated in every prompt.

Keep `AGENTS.md` aligned with its actual contents, including protected legacy contracts, architecture boundaries, persistence/money/security/logging/concurrency rules and verification expectations.

## Prompt workflow

Summarize actual prompts by phases instead of explaining every prompt:

```text
1. Understand and protect
   - onboarding
   - characterization

2. Establish boundaries
   - infrastructure
   - OpenAPI
   - Hexagonal Architecture
   - persistence

3. Implement behavior
   - interest policies
   - application use cases
   - REST adapters

4. Harden
   - security
   - logging
   - integration/E2E
   - demo data
   - pagination
   - bulk update/idempotency/concurrency

5. Review
   - evaluator/submission review
   - README simplification
```

Use actual filenames where useful. Do not fabricate missing/executed prompts.

## Development loop

Document:

```text
Analyze
  ↓
Identify invariants / ambiguities
  ↓
Protect existing behavior with tests
  ↓
Make the smallest design change
  ↓
Implement
  ↓
Run focused tests
  ↓
Run integration/runtime checks
  ↓
Review against repository constraints
```

Mention that ambiguities were surfaced rather than silently invented. A compact example, if consistent with final implementation:

```text
days -> eligibility
accrual period -> idempotency
```

## Verification guardrails

Describe only mechanisms actually present/used:

- legacy characterization tests
- OpenAPI contract
- Hexagonal boundaries
- PostgreSQL/Flyway
- Testcontainers
- API/E2E tests
- concurrency/idempotency tests
- rollback/retry tests
- Maven build
- Docker smoke test
- OWASP Dependency-Check

Do not claim a check passed if it was not actually executed successfully.

## Human ownership

Explain briefly:

- AI-generated code was treated as a proposal
- business/architecture assumptions were explicitly reviewed
- legacy behavior was protected by characterization tests
- unnecessary abstractions were rejected
- final correctness remained the engineer's responsibility

Avoid promotional language.

## Reproducibility

Explain how another engineer can reproduce the workflow:

1. clone repository
2. read `AGENTS.md`
3. inspect relevant source-of-truth docs
4. execute/select focused prompts under `docs/prompts/`
5. review diffs
6. run verification

Use actual valid commands where useful:

```bash
cd kotlin
./mvnw clean test
./mvnw dependency-check:check
docker compose up --build
```

## Skills

Do not claim a custom Codex Skill was used unless one actually exists and was used.

If none was used, a short future-looking note is acceptable:

> Project-specific constraints remain in version-controlled repository files. Reusable cross-project procedures could be extracted into Codex Skills, while business invariants remain in `AGENTS.md` and project documentation.

Do not add Skills just to make the submission look sophisticated.

## Suggested document

```text
# AI-Assisted Development
## Approach
## Repository as the Harness
## Prompt Workflow
## Development Loop
## Verification Guardrails
## Human Review
## Reproducing the Workflow
```

Target roughly 2-4 Markdown pages, not a giant manual.

Ensure README contains only a compact AI summary pointing to `docs/ai-assisted-development.md`.

## Assignment compliance

Verify this document satisfies:

- tools/setup documented
- practical reproducible workflow
- custom rules / agent configuration referenced
- prompts referenced
- AI-assisted areas summarized
- rationale/verification explained

Before editing report AI artifacts found, prompt files found, verification mechanisms found, whether any custom Skills were actually used, and proposed outline.

After editing report files changed, document structure, how the assignment AI requirement is satisfied, unconfirmed verification steps, and confirmation no application behavior changed.

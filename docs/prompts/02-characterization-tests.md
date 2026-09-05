# 02 - Characterize Legacy Behavior

Read:

- `AGENTS.md`
- `README.md`
- `docs/architecture-guidelines.md`
- the existing `TimeDeposit`
- the existing `TimeDepositCalculator`
- the existing tests

## Goal

Protect the observable behavior of the existing
`TimeDepositCalculator.updateBalance` implementation before any refactoring.

Do not modify production code.

Do not add Spring, persistence, Docker, Testcontainers, or any other infrastructure yet.

## Required Tests

Replace or extend the placeholder tests with characterization tests covering the current implementation.

### General

Cover:

- empty list
- mutation of the supplied `TimeDeposit`
- unknown plan type
- case sensitivity of `planType`

### Basic Plan

Cover:

- 30 days
- 31 days
- representative value above 30 days

Expected current rule:

- no interest at `days <= 30`
- monthly interest of `1% / 12` when `days > 30`

### Student Plan

Cover:

- 30 days
- 31 days
- 365 days
- 366 days

Expected current rule:

- no interest at `days <= 30`
- monthly interest of `3% / 12` for `30 < days < 366`
- no interest at `days >= 366`

### Premium Plan

Cover:

- 30 days
- 31 days
- 45 days
- 46 days

Expected current rule:

- no interest through day 45
- monthly interest of `5% / 12` when `days > 45`

### Rounding

Add at least one test that locks down the existing rounding behavior:

- interest is rounded to two decimal places
- rounding mode is HALF_UP
- preserve the existing Double -> BigDecimal -> Double behavior

Do not replace the existing monetary representation in production code.

## Test Style

Prefer:

- descriptive test names
- Arrange / Act / Assert structure where useful
- AssertJ or the existing test stack
- direct assertions against observable behavior

Do not test private implementation details.

Avoid parameterization if it makes the business boundaries harder to read.

## Verification

After implementation:

1. Run the test suite if Maven is available.
2. If Maven is unavailable, report that clearly.
3. Do not change production code to make a characterization test pass.
4. Compare every expected value against the existing implementation.

## Output

Before editing, briefly list the test cases you will add.

After editing, report:

1. files changed
2. behavior characterized
3. tests executed
4. any surprising legacy behavior discovered
5. any ambiguity that should be decided before refactoring
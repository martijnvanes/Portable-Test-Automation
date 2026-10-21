---
paths:
  - "src/main/java/**/steps/**"
  - "src/main/java/**/util/validation/**"
  - "src/main/java/**/util/helpers/**"
---

# Validation strength

Every property of an expected result is validated at the strongest level that is possible. Only drop a level when the level above is impossible, and first try to make the value predictable.

1. **Exact value** (the default, needs no justification). The expected holds the full value and has the same type as the actual.
2. **Partial value**: contains, starts with, ends with.
3. **Regex**: the whole value must match.
4. **Empty / not empty**: the value must be (or must not be) null, an empty string, collection, map or array.
5. **Ignore**: last resort, the property is not validated.

Levels 2 and 3 are not strictly ordered. A tight regex can be stronger than a weak contains. When an exact match is impossible, **combining** a partial check and a regex on the same property is stronger than either alone. For example, `startsWith("order-")` together with a regex for the generated part. Prefer the combination over a single check when both can be expressed, and report every check of the combination.

## Rules

- **Make the value predictable before weakening the check.** Reuse a generated id from the POST response in the GET expectation, use a before/after range for a timestamp, create data with a unique marker.
- **`null` in the expected means "must be null".** It is not a way to skip a property. Skipping is level 5 and is always named explicitly.
- **Combine before you weaken.** If exact is impossible, first try partial plus regex together, and only then a single partial, a single regex, empty/not empty or ignore.
- **Every check below level 1 gets a reason** and must be visible in the Cucumber report (log the property, the level and the reason). Ignoring a property without a reason is not allowed.
- **A weaker check must name the property it applies to.** If that property is never validated (typo, renamed field), the validation must fail instead of silently passing.
- **Keep it soft asserted.** A weaker check is still part of the one soft assertion report, with a single `assertAll()` at the end of the validation.
- **Don't drop a level for convenience.** Needing level 5 for a property is a prompt to ask whether the test data or the scenario can be changed so level 1 works.

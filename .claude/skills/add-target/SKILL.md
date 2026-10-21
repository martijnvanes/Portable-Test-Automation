---
name: add-target
description: Add a new API under test (a "target") to the TAS, or add the first scenarios for a target that is configured but has none yet (WordPress, weather). Use when the user wants to test a new API, add a target, or write scenarios for wordpress/weather.
---

# Add a target to the TAS

A target is an external API the TAS runs Cucumber scenarios against. Each target needs config,
models, a messaging class, a validation class, a step class, a feature file, a test of the
service, and documentation. Petstore is the reference implementation: read
`steps/petstore/PetStoreSteps.java`, `util/messaging/MessagingPetstore.java`,
`util/validation/PetStoreValidation.java` and `features/petstore/pets.feature` before writing
anything, and follow their style. Also read `CLAUDE.md` (Step structure, Validations) and
`.claude/rules/validation-strength.md`.

## 0. Ask first

If the user did not say, ask:
- the target name (lowercase, one word; it becomes the tag, package and env var), and its base URL
- which behaviour the first scenario should check (keep it to one or two scenarios)

Then explore the real API (fetch an endpoint or its docs) so the scenario asserts on fields that actually exist.

## 1. Check what already exists

WordPress and weather are already configured. For each step below, check first and skip it if it is done:

| Step | File | Already done for |
|------|------|------------------|
| 2 | `config/TasProperties.java` | wordpress, weather (petstore has no base-url config) |
| 3 | `src/main/resources/application.yml` | wordpress, weather (petstore has no base-url config) |
| 7 | `deploy/synology/docker-compose.yml` | wordpress, weather (petstore has no base-url config) |

## 2. Config record

Add a `Target <name>` component to `TasProperties.Targets`. Record components bind by name to
`tas.targets.<name>`.

## 3. application.yml

Under `tas.targets`, add `<name>.base-url: ${<NAME>_BASE_URL:<public default>}`. The env var
override is what makes the image portable; the default keeps local runs working without setup.

## 4. Step class

Create the pieces below, in the packages `CLAUDE.md` describes:
- **Models** in `models/<name>` (an enum goes in `enums/`).
- **`util/messaging/Messaging<Name>`**: the HTTP calls with RestAssured. Build the base URL from
  `TestTargets.current().<name>().baseUrl()` (Cucumber creates the steps, so there is no Spring
  injection; that is why they read `TestTargets`). Do not hard code the URL, or the env var
  override is bypassed. Do not assert the status code in the
  messaging class, a scenario may expect a 404; the validation class checks it.
- **`util/helpers/<name>`**: creation of uniquely marked test data (random id, name or tag).
- **`util/validation/<Name>Validation`**: compares expected and actual of the same type through
  `AssertionHelper` (soft assertions, one `assertAll()` at the end). Decide the match criteria per
  validation, make leftover actuals fail, and follow `.claude/rules/validation-strength.md`.
  Register it in `BaseTest`.
- **`steps/<name>/<Name>Steps`** extends `CucumberScenario`, takes `BaseTest` in its constructor
  (injected by cucumber-picocontainer) and keeps shared state in `baseTest.scenarioContext`.
  Keep steps thin: gather data, call messaging, hand expected and actual to the validation class.
- **Step texts must be unique across ALL step classes.** Cucumber's glue is global: a second
  `the response status is {int}` gives a `DuplicateStepDefinitionException`, and reusing the
  Petstore one would check the Petstore class's `response` field, not yours. Phrase steps in the
  target's domain ("the posts response status is 200", "the forecast contains 24 hours").
- Public APIs are shared and can change: assert on structure and stable values, not on content
  that changes daily (post titles, temperatures).

## 5. Feature file

Create `src/main/resources/features/<name>/<topic>.feature`, tagged `@<name>` at feature level
so `POST /api/runs?tags=@<name>` selects it. Write scenarios in business language, like `pets.feature`.

## 6. Test of the service

`TestRunApiTests` runs the real feature files against the real APIs; there are no fakes or
simulated environments, on purpose. It therefore needs network access, and so does CI. For the
new target add a test that posts `tags=@<name>` and asserts PASSED, the scenario count and the
report.

## 7. Docker compose

Add `<NAME>_BASE_URL` to the `environment` section of `deploy/synology/docker-compose.yml`.
Remind the user that the NAS copy of this file must be updated too.

## 8. Verify

Run `.\mvnw.cmd test` (PowerShell) and make sure everything passes. Because the tests hit
the real API, a failure can also come from the public API being down or changed; check that
before changing code.

## 9. Documentation

Update `CLAUDE.md`: the sentence listing targets without scenarios, and the "Adding a new API
target" paragraph if this skill revealed something it misses.

## Finish

Summarise which files changed and why, and that deploying means push, wait for the build, then
`sudo docker compose pull && sudo docker compose up -d` on the Synology. Do not commit or push
unless the user asks.

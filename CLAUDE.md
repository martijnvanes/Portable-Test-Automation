# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A "TAS" (test automation solution): a Spring Boot service, shipped as a Docker image, that runs Cucumber + RestAssured API tests when you call its REST API and publishes Masterthought (cucumber-reporting) HTML reports. It is a personal learning project, so explain design choices instead of only making them.

## Goals and scope

- Tests REST APIs only for now (no Selenium yet), written as Java code with Cucumber + RestAssured + JUnit.
- Runs are triggered through the TAS's own REST endpoints, one at a time and synchronously (`POST /api/runs` waits for the result). Ask before switching to async.
- Results go into a Masterthought report.
- Targets: Swagger Petstore (has scenarios, fixed URL, no config), the public WordPress REST API and open weather-forecast APIs (configured, no scenarios yet).
- Target URLs and credentials come from environment variables; that is what makes it portable.
- These requirements may change. Keep the design simple and leave room for Selenium, parallel runs and more users.

## Commands

Java 21, Maven via the wrapper (no global `mvn`). In PowerShell use `.\mvnw.cmd` instead of `./mvnw`.

```bash
./mvnw test                                    # all service tests (src/test)
./mvnw test -Dtest=TestRunApiTests             # one test class
./mvnw test -Dtest=TestRunApiTests#reportsNoTestsWhenNoScenarioMatchesTheTags   # one method
./mvnw spring-boot:run                         # start locally on :8080, Swagger UI at /
curl -X POST "http://localhost:8080/api/runs?tags=@petstore"   # trigger a run
```

There is no Docker on the development laptop; the Dockerfile is only exercised by GitHub Actions.

## Architecture

**Two kinds of "tests" live in this repo, don't mix them up:**
- `src/main/resources/features` + `src/main/java/.../steps`: the *product*, the Cucumber scenarios the service runs against external APIs. They are in `src/main` (and Cucumber/RestAssured/JUnit Platform have compile scope) because they are packaged into the jar and executed at runtime.
- `src/test`: ordinary tests of the service itself. `TestRunApiTests` runs the real feature files through `POST /api/runs` against the real public Petstore. There are no fakes or simulated environments on purpose, so these tests (and the CI build) need network access.

**Run flow:** `TestRunController` → `TestRunService.run(tags)` builds a JUnit Platform `LauncherDiscoveryRequest` for the `cucumber` engine (features from classpath `features`, glue `org.example.tas.steps`, JSON plugin), executes it in-process, then turns the JSON into a Masterthought report under `<reports-dir>/<runId>/`. `ReportsWebConfig` serves that directory at `/reports/**`; the response contains the `reportUrl`. Runs are synchronous and `run` is `synchronized`.

**Config reaches the steps through static state:** Cucumber instantiates step classes itself, so they can't get Spring beans. `TestRunService` puts `TasProperties.Targets` into `TestTargets` before each run and steps read `TestTargets.current()`. If nothing was set (a scenario run straight from the IDE), `TestTargets` falls back to binding `tas.targets` from `application.yml`, env vars included. This is why runs must stay serialized; changing to parallel runs means replacing this mechanism.

**Step structure:** step classes extend `util/scenario/CucumberScenario` and take a `BaseTest` in their constructor. `cucumber-picocontainer` creates one `BaseTest` per scenario and injects the same instance into every step and hook class of that scenario. `BaseTest` holds the `ScenarioContext` (state shared between steps: the pet, the last response, ...) and the validation classes (`PetStoreValidation`). `CucumberScenario`'s constructor calls `AssertionHelper.reset()`, so every scenario starts with fresh soft assertions. HTTP calls live in `util/messaging/Messaging<Target>`, test data creation in `util/helpers/<target>/`. Steps stay thin: gather data, call messaging, hand expected and actual to a validation class.

**Adding a new API target** touches several places: a `Target` field in `TasProperties.Targets`, a `tas.targets.<name>.base-url: ${<NAME>_BASE_URL:...}` entry in `application.yml`, a `steps/<name>` package, a `Messaging<Name>` class, models in `models/<name>`, a `<Name>Validation` class in `util/validation`, a feature file tagged `@<name>`, and the env var in `deploy/synology/docker-compose.yml`. The `add-target` skill (`.claude/skills/add-target`) walks through this. WordPress and weather targets are configured but have no scenarios yet.

All target URLs and the reports dir are overridable with environment variables; that is what makes the image portable.

**Swagger UI** (springdoc 2.x; 3.x would require Spring Boot 4) is generated from the controllers; `/` redirects to it. New endpoints appear there automatically.

## Code layout

Under `org.example.tas`: all enumerations go in `enums/`, all models in `models/` (one sub package per target), and helpers in `util/<concern>/` (one sub package per concern: `validation`, `messaging`, `helpers`, `scenario`). New enums and models go there, not next to the class that uses them. `run/RunResult` and the nested config classes in `TasProperties` predate this and have not been moved.

## Validations

Steps validate through the per-target classes in `util/validation` (for example `PetStoreValidation`), which assert through `util/helpers/AssertionHelper`, not with ad-hoc RestAssured `equalTo` assertions. The rules:

- The step gathers an expected and an actual of the **same type** (models in `models/`); the validation method just compares them.
- **Everything is soft asserted into one report** (a single `assertAll()` at the end of the validation), so one run shows as many failures as possible. Don't add early hard asserts in the middle of a validation.
- **Lists:** iterate over the expecteds and select the corresponding actual(s) with a match predicate that is decided per validation, never stored on the model. Work on a copy of the actuals, never mutate the caller's list. Per expected: no candidate is a failure ("no actual found"); one candidate is validated and removed; several identical candidates take one and remove only that one; several candidates that differ are a failure ("ambiguous, tighten the filter").
- **Leftover actuals** (no expected claimed them) must be empty, always. That check never switches off and sits in the same soft assertion as everything else.
- **Label assertions** (for example `pet[id=123].name`) so the Masterthought report is readable.
- **Validation strength** (exact value first, then partial, regex, empty/not empty, ignore with a reason) is described in `.claude/rules/validation-strength.md`.
- **Test data must be accurate on a shared system.** Create uniquely marked data (random id, name or tag) and narrow the actuals to it before the strict validation; never expect "only my new pet" from a get-all call.

## Conventions

- **Commit messages:** imperative, capitalised subject without prefix or trailing period (about 72 characters or fewer). For anything beyond a trivial change add a body, wrapped around 72 characters, that says what changed and why. Keep unrelated changes in separate commits. Base the message on the actual diff.
- Mind that a merge to `master` triggers the Docker image build (see below).

## Build and deployment

- Push to `master` → `.github/workflows/docker-publish.yml` runs `./mvnw verify`, then builds a linux/amd64 + linux/arm64 image and pushes `ghcr.io/martijnvanes/portable-test-automation` (`latest` + `sha-…`). Changes only under `deploy/**` or `.claude/**` don't trigger it.
- Changes reach `master` only through pull requests from a branch; `master` is protected on GitHub, so direct pushes are rejected. Before the first commit of a change, branch off an up-to-date `master` (`git pull --ff-only`, then `git checkout -b <type>/<short-description>` with type `feature`, `fix`, `docs` or `chore`).
- A pull request runs only the `test` job (`./mvnw verify`), which is the required status check: renaming that job in the workflow makes every pull request wait forever for a check that never reports. The image is built and published after the merge.
- Merge with a merge commit (`gh pr merge --merge --delete-branch`) so the history keeps showing the pull request. Never merge a pull request without being asked, and ask first whether a pull request is wanted. `.claude/settings.json` has `ask` rules for `git push` and `gh pr`, so expect a prompt there.
- Merging does not update the Synology: once the image build is done, run the manual update described in the last bullet below.
- The GHCR image is private on purpose; the Synology logs in with a token.
- It runs on the user's Synology as Container Manager project `tas` from `deploy/synology/docker-compose.yml`, on host port **8081** (8080 is taken on the NAS). Keep that file in sync with what runs on the NAS.
- After a successful build, the Synology does *not* pick up the new image via Container Manager's Build action. Update over SSH in the project folder: `sudo docker compose pull && sudo docker compose up -d`.

## Gotchas

- `mvnw` must stay executable (git mode 100755) with LF line endings (`.gitattributes`), or the Linux CI/Docker build fails.
- GHCR image names must be lowercase, so the workflow doesn't derive the name from the repository name (`Portable-Test-Automation`).
- Petstore has no `tas.targets` entry or base-url env var on purpose: `MessagingPetstore` builds its URL from the constants `ENV_PREFIX` and `ENV_INFIX` in `TasProperties` and always hits the real public Petstore.
- The public Petstore is load balanced and shared: steps use random large ids and retry reads (`getPetExpecting`).
- In `TestRunService`, "skipped" is the aborted count, because Cucumber reports tag-filtered scenarios as skipped.

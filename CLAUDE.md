# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A "TAS" (test automation solution): a Spring Boot service, shipped as a Docker image, that runs Cucumber + RestAssured API tests when you call its REST API and publishes Masterthought (cucumber-reporting) HTML reports. It is a personal learning project, so explain design choices instead of only making them.

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
- `src/test`: ordinary tests of the service itself. `TestRunApiTests` runs the real feature files through `POST /api/runs` against an in-process fake Petstore (`com.sun.net.httpserver`), so it needs no network.

**Run flow:** `TestRunController` → `TestRunService.run(tags)` builds a JUnit Platform `LauncherDiscoveryRequest` for the `cucumber` engine (features from classpath `features`, glue `org.example.tas.steps`, JSON plugin), executes it in-process, then turns the JSON into a Masterthought report under `<reports-dir>/<runId>/`. `ReportsWebConfig` serves that directory at `/reports/**`; the response contains the `reportUrl`. Runs are synchronous and `run` is `synchronized`.

**Config reaches the steps through static state:** Cucumber instantiates step classes itself, so they can't get Spring beans. `TestRunService` puts `TasProperties.Targets` into `TestTargets` before each run and steps read `TestTargets.current()`. This is why runs must stay serialized; changing to parallel runs means replacing this mechanism.

**Adding a new API target** touches several places: a `Target` field in `TasProperties.Targets`, a `tas.targets.<name>.base-url: ${<NAME>_BASE_URL:...}` entry in `application.yml`, a `steps/<name>` package with a `@Before("@<name>")` hook that builds the RestAssured spec, a feature file tagged `@<name>`, and the env var in `deploy/synology/docker-compose.yml`. WordPress and weather targets are configured but have no scenarios yet.

All target URLs and the reports dir are overridable with environment variables; that is what makes the image portable.

**Swagger UI** (springdoc 2.x; 3.x would require Spring Boot 4) is generated from the controllers; `/` redirects to it. New endpoints appear there automatically.

## Build and deployment

- Push to `master` → `.github/workflows/docker-publish.yml` runs `./mvnw verify`, then builds a linux/amd64 + linux/arm64 image and pushes `ghcr.io/martijnvanes/portable-test-automation` (`latest` + `sha-…`). Changes only under `deploy/**` or `.claude/**` don't trigger it.
- The GHCR image is private on purpose; the Synology logs in with a token.
- It runs on the user's Synology as Container Manager project `tas` from `deploy/synology/docker-compose.yml`, on host port **8081** (8080 is taken on the NAS). Keep that file in sync with what runs on the NAS.
- After a successful build, the Synology does *not* pick up the new image via Container Manager's Build action. Update over SSH in the project folder: `sudo docker compose pull && sudo docker compose up -d`.

## Gotchas

- `mvnw` must stay executable (git mode 100755) with LF line endings (`.gitattributes`), or the Linux CI/Docker build fails.
- GHCR image names must be lowercase, so the workflow doesn't derive the name from the repository name (`Portable-Test-Automation`).
- The public Petstore is load balanced and shared: steps use random large ids and retry reads (`getPetExpecting`).
- In `TestRunService`, "skipped" is the aborted count, because Cucumber reports tag-filtered scenarios as skipped.

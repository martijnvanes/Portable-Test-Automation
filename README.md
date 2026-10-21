# Portable Test Automation

A test automation solution (TAS): a Spring Boot service, shipped as a Docker image, that runs
Cucumber + RestAssured API tests when you call its REST API and publishes Masterthought HTML reports.

## Quick start

Java 21 is required. Maven comes with the wrapper (`./mvnw`, on Windows PowerShell `.\mvnw.cmd`).

```bash
./mvnw test                                                    # tests of the service itself
./mvnw spring-boot:run                                         # start on :8080, Swagger UI at /
curl -X POST "http://localhost:8080/api/runs?tags=@petstore"   # trigger a run
```

The response contains a `reportUrl`, served by the service under `/reports/`.

## Configuration

Target URLs and the reports directory are set with environment variables (defaults in
`src/main/resources/application.yml`), for example `WORDPRESS_BASE_URL`. See
`deploy/synology/docker-compose.yml` for a deployment example.

## More documentation

- `CLAUDE.md`: architecture, run flow, code layout, validation rules, conventions, build and
  deployment. Written for Claude Code, but it is the project documentation for people too.
- `.claude/rules/validation-strength.md`: how strictly each property of a result must be validated.
- `.claude/skills/add-target/SKILL.md`: step by step guide to add a new API under test.

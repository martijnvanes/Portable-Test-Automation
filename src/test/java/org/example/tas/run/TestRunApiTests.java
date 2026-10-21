package org.example.tas.run;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real feature files through the REST API against the real public Petstore,
 * so the whole flow (endpoint, Cucumber run, Masterthought report) needs network access.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestRunApiTests {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("tas.reports-dir", () -> "target/test-reports");
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    void runsPetstoreFeaturesAndPublishesReport() {
        RunResult result = rest.postForObject("/api/runs?tags={tags}", null, RunResult.class, "@petstore");

        assertThat(result.status()).isEqualTo(RunResult.Status.PASSED);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.passed()).isEqualTo(2);
        assertThat(result.reportUrl()).isNotNull();

        ResponseEntity<String> report = rest.getForEntity(result.reportUrl(), String.class);
        assertThat(report.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(report.getBody()).contains("Pets in the Swagger Petstore");
    }

    @Test
    void reportsNoTestsWhenNoScenarioMatchesTheTags() {
        RunResult result = rest.postForObject("/api/runs?tags={tags}", null, RunResult.class, "@does-not-exist");

        assertThat(result.status()).isEqualTo(RunResult.Status.NO_TESTS);
        assertThat(result.total()).isZero();
        assertThat(result.reportUrl()).isNull();
    }
}

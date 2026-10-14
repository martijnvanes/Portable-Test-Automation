package org.example.tas.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.tas.run.RunResult;
import org.example.tas.run.TestRunService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Starts test runs, e.g. {@code POST /api/runs?tags=@petstore}.
 * The call blocks until the run is finished and returns the summary with a link to the report.
 */
@RestController
@RequestMapping("/api/runs")
@Tag(name = "Test runs")
public class TestRunController {

    private final TestRunService testRunService;

    public TestRunController(TestRunService testRunService) {
        this.testRunService = testRunService;
    }

    @PostMapping
    @Operation(summary = "Run the test scenarios",
            description = "Waits until the run is finished. Open the returned reportUrl on this server to see the report.")
    public RunResult run(
            @Parameter(description = "Cucumber tag expression; leave empty to run all scenarios", example = "@petstore")
            @RequestParam(required = false) String tags) {
        return testRunService.run(tags);
    }
}

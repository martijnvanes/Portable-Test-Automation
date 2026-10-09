package org.example.tas.api;

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
public class TestRunController {

    private final TestRunService testRunService;

    public TestRunController(TestRunService testRunService) {
        this.testRunService = testRunService;
    }

    @PostMapping
    public RunResult run(@RequestParam(required = false) String tags) {
        return testRunService.run(tags);
    }
}

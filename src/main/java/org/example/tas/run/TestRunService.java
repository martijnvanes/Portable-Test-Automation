package org.example.tas.run;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.example.tas.config.TasProperties;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PUBLISH_QUIET_PROPERTY_NAME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource;
import static org.junit.platform.launcher.EngineFilter.includeEngines;

/**
 * Runs the Cucumber features packaged in the application through the JUnit Platform
 * and turns the Cucumber JSON output into a Masterthought HTML report.
 */
@Service
public class TestRunService {

    private static final Logger log = LoggerFactory.getLogger(TestRunService.class);

    private static final String REPORT_TITLE = "Portable Test Automation";
    private static final String FEATURES = "features";
    private static final String GLUE = "org.example.tas.steps";
    private static final DateTimeFormatter RUN_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final TasProperties properties;

    public TestRunService(TasProperties properties) {
        this.properties = properties;
    }

    /**
     * Runs the scenarios matching {@code tags} (a Cucumber tag expression), or all of them when blank.
     * Synchronized because the step definitions read the targets from shared static state.
     */
    public synchronized RunResult run(String tags) {
        String runId = LocalDateTime.now().format(RUN_ID_TIME) + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path runDir = Path.of(properties.reportsDir(), runId).toAbsolutePath();
        Path json = runDir.resolve("cucumber.json");
        createDirectories(runDir);

        TestTargets.set(properties.targets());
        log.info("Starting run {} with tags '{}'", runId, tags);

        LauncherDiscoveryRequestBuilder request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClasspathResource(FEATURES))
                .filters(includeEngines("cucumber"))
                .configurationParameter(GLUE_PROPERTY_NAME, GLUE)
                .configurationParameter(PLUGIN_PROPERTY_NAME, "json:" + json)
                .configurationParameter(PLUGIN_PUBLISH_QUIET_PROPERTY_NAME, "true");
        if (tags != null && !tags.isBlank()) {
            request.configurationParameter(FILTER_TAGS_PROPERTY_NAME, tags);
        }

        TestExecutionSummary summary = execute(request.build());
        long total = summary.getTestsStartedCount();
        long passed = summary.getTestsSucceededCount();
        long failed = summary.getTestsFailedCount();
        // Not getTestsSkippedCount(): Cucumber reports scenarios excluded by the tag filter as skipped
        long skipped = summary.getTestsAbortedCount();

        RunResult.Status status = total == 0 ? RunResult.Status.NO_TESTS
                : failed > 0 ? RunResult.Status.FAILED
                : RunResult.Status.PASSED;
        String reportUrl = total == 0 ? null : generateReport(runId, tags, runDir, json);

        log.info("Finished run {}: {} ({} passed, {} failed, {} skipped)", runId, status, passed, failed, skipped);
        return new RunResult(runId, tags, status, total, passed, failed, skipped, reportUrl);
    }

    private static TestExecutionSummary execute(LauncherDiscoveryRequest request) {
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request, listener);
        return listener.getSummary();
    }

    private static String generateReport(String runId, String tags, Path runDir, Path json) {
        Configuration configuration = new Configuration(runDir.toFile(), REPORT_TITLE);
        configuration.setBuildNumber(runId);
        configuration.addClassifications("Tags", tags == null || tags.isBlank() ? "(all)" : tags);
        new ReportBuilder(List.of(json.toString()), configuration).generateReports();
        return "/reports/" + runId + "/cucumber-html-reports/overview-features.html";
    }

    private static void createDirectories(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create report directory " + dir, e);
        }
    }
}

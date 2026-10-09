package org.example.tas.run;

/**
 * Outcome of one test run, returned by the REST API.
 *
 * @param reportUrl path to the Masterthought overview, or {@code null} when no scenarios ran
 */
public record RunResult(
        String runId,
        String tags,
        Status status,
        long total,
        long passed,
        long failed,
        long skipped,
        String reportUrl) {

    public enum Status { PASSED, FAILED, NO_TESTS }
}

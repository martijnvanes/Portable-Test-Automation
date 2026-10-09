package org.example.tas.run;

import org.example.tas.config.TasProperties;

/**
 * Hands the configured target URLs to the step definitions.
 * Cucumber creates the step classes itself, so they cannot receive Spring beans;
 * {@link TestRunService} sets this before each run (runs are serialized).
 */
public final class TestTargets {

    private static volatile TasProperties.Targets current;

    private TestTargets() {
    }

    static void set(TasProperties.Targets targets) {
        current = targets;
    }

    public static TasProperties.Targets current() {
        if (current == null) {
            throw new IllegalStateException("No targets configured; start runs through TestRunService");
        }
        return current;
    }
}

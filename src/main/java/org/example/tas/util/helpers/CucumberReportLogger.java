package org.example.tas.util.helpers;

import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;

@Slf4j
public class CucumberReportLogger {
    private static final ThreadLocal<Scenario> SCENARIO = new ThreadLocal<>();

    private CucumberReportLogger() {
        // static utility class
    }

    public static void setScenario(Scenario scenario) {
        SCENARIO.set(scenario);
    }

    public static Scenario getScenario() {
        return SCENARIO.get();
    }

    public static void clearScenario() {
        SCENARIO.remove();
    }

    public static void logForCucumberReport(String message) {
        Scenario currentScenario = SCENARIO.get();
        if (!isNull(currentScenario)) {
            currentScenario.log(message);
        } else {
            log.info(message);
        }
    }
}

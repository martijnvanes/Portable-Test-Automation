package org.example.tas.util.scenario;

import io.cucumber.messages.types.Scenario;
import org.example.tas.util.BaseTest;

import org.example.tas.util.helpers.AssertionHelper.reset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

public class CucumberScenario {
    protected final BaseTest baseTest;
    public static ThreadLocal<Scenario> scenario = new ThreadLocal<>();
    public CucumberScenario(BaseTest baseTest) {
        this.baseTest = baseTest;
        reset;
    }
}

package org.example.tas.util.scenario;

import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.example.tas.run.TestTargets;
import org.example.tas.util.BaseTest;

@Slf4j
public class SetupAndTeardownCucumberScenario {
    protected final BaseTest baseTest;
    protected final ScenarioContext scenarioContext;

    public SetupAndTeardownCucumberScenario(BaseTest baseTest) {
        this.baseTest = baseTest;
        scenarioContext = baseTest.scenarioContext;
    }

    @BeforeAll
    public static void beforeAll() {
        // before all
    }

    @Before(order = 0, value = "@petstore")
    public void setUp(Scenario scenario) {
        log.debug("Setting up petstore ....");
    }
}

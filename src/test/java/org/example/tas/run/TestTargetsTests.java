package org.example.tas.run;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestTargetsTests {

    @AfterEach
    void resetStaticState() {
        TestTargets.clear();
    }

    @Test
    void fallsBackToApplicationYmlWhenNothingWasSet() {
        TestTargets.clear();

        assertThat(TestTargets.current().petstore().baseUrl()).isNotBlank();
        assertThat(TestTargets.current().wordpress().baseUrl()).isNotBlank();
        assertThat(TestTargets.current().weather().baseUrl()).isNotBlank();
    }
}

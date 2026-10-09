package org.example.tas;

import org.example.tas.config.TasProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "tas.targets.petstore.base-url=http://localhost:9999/v2")
class TasApplicationTests {

    @Autowired
    private TasProperties properties;

    @Test
    void contextLoads() {
        assertThat(properties.reportsDir()).isEqualTo("reports");
    }

    @Test
    void targetBaseUrlsCanBeOverridden() {
        assertThat(properties.targets().petstore().baseUrl()).isEqualTo("http://localhost:9999/v2");
        assertThat(properties.targets().weather().baseUrl()).isEqualTo("https://api.open-meteo.com/v1");
    }
}

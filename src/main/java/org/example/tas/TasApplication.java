package org.example.tas;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(title = "Portable Test Automation",
        description = "Runs Cucumber/RestAssured API tests on request and publishes Masterthought reports"))
public class TasApplication {

    public static void main(String[] args) {
        SpringApplication.run(TasApplication.class, args);
    }
}

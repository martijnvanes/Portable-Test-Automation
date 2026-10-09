package org.example.tas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under the {@code tas} prefix in application.yml.
 * Every value can be overridden with an environment variable, so the same
 * Docker image can be pointed at different environments.
 */
@ConfigurationProperties(prefix = "tas")
public record TasProperties(String reportsDir, Targets targets) {

    public record Targets(Target petstore, Target wordpress, Target weather) {
    }

    public record Target(String baseUrl) {
    }
}

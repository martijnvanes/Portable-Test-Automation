package org.example.tas.config;

import io.restassured.RestAssured;
import io.restassured.config.*;
import io.restassured.path.json.config.JsonPathConfig;
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

    // Environment varialbles
    public static final String ENV_PREFIX = "https://";
    public static String ENV_INFIX = "petstore.swagger.io/v2/";

    // Rest Assured configuration
    public static final RestAssuredConfig config = RestAssured.config().httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.manager.timeout", 130000)
                    .setParam("http.connection.timeout", 130000)
                    .setParam("http.socket.timeout", 130000))
            .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails())
            .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
            .jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
}

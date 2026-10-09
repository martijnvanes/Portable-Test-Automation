package org.example.tas.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real feature files through the REST API against an in-process fake Petstore,
 * so the whole flow (endpoint, Cucumber run, Masterthought report) is tested without network access.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestRunApiTests {

    private static final HttpServer fakePetstore = startFakePetstore();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("tas.targets.petstore.base-url",
                () -> "http://localhost:" + fakePetstore.getAddress().getPort() + "/v2");
        registry.add("tas.reports-dir", () -> "target/test-reports");
    }

    @AfterAll
    static void stopFakePetstore() {
        fakePetstore.stop(0);
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    void runsPetstoreFeaturesAndPublishesReport() {
        RunResult result = rest.postForObject("/api/runs?tags={tags}", null, RunResult.class, "@petstore");

        assertThat(result.status()).isEqualTo(RunResult.Status.PASSED);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.passed()).isEqualTo(2);
        assertThat(result.reportUrl()).isNotNull();

        ResponseEntity<String> report = rest.getForEntity(result.reportUrl(), String.class);
        assertThat(report.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(report.getBody()).contains("Pets in the Swagger Petstore");
    }

    @Test
    void reportsNoTestsWhenNoScenarioMatchesTheTags() {
        RunResult result = rest.postForObject("/api/runs?tags={tags}", null, RunResult.class, "@does-not-exist");

        assertThat(result.status()).isEqualTo(RunResult.Status.NO_TESTS);
        assertThat(result.total()).isZero();
        assertThat(result.reportUrl()).isNull();
    }

    /** Minimal in-memory Petstore: POST /v2/pet, GET and DELETE /v2/pet/{id}. */
    private static HttpServer startFakePetstore() {
        ObjectMapper mapper = new ObjectMapper();
        Map<Long, Map<String, Object>> pets = new ConcurrentHashMap<>();
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/v2/pet", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();
                if (path.equals("/v2/pet") && method.equals("POST")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pet = mapper.readValue(exchange.getRequestBody(), Map.class);
                    pets.put(((Number) pet.get("id")).longValue(), pet);
                    respond(exchange, 200, mapper.writeValueAsString(pet));
                    return;
                }
                long id = Long.parseLong(path.substring("/v2/pet/".length()));
                Map<String, Object> pet = method.equals("DELETE") ? pets.remove(id) : pets.get(id);
                if (pet == null) {
                    respond(exchange, 404, "{\"message\":\"Pet not found\"}");
                } else {
                    respond(exchange, 200, mapper.writeValueAsString(pet));
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}

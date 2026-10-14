package org.example.tas.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiDocsTests {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void openApiDescribesTheRunEndpoint() {
        String docs = rest.getForObject("/v3/api-docs", String.class);

        assertThat(docs).contains("/api/runs").contains("Portable Test Automation");
    }

    @Test
    void rootUrlOpensSwaggerUi() {
        ResponseEntity<String> page = rest.getForEntity("/", String.class);

        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody()).contains("swagger-ui");
    }
}

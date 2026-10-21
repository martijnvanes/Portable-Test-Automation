package org.example.tas.config;

import io.restassured.specification.RequestSpecification;

public class BaseConfiguration {
    public static final long READ_DELAY_MS = 500;
    public static final int READ_ATTEMPTS = 5;
}

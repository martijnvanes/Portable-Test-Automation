package org.example.tas.util.messaging;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.example.tas.models.petstore.Pet;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.example.tas.config.TasProperties.*;

public class petstore {
    // Controller PET
    public static Response postPet(Pet pet) {
        return given().config(config).baseUri(ENV_PREFIX + ENV_INFIX + "pet")
                .log().uri()
                .when()
                .contentType(ContentType.JSON)
                .body(pet)
                .post()
                .then()
                .statusCode(SC_OK)
                .extract().response();
    }

    // Controller STORE

    // Controller USER
}

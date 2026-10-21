package org.example.tas.util.helpers.petstore;

import io.restassured.response.Response;
import org.example.tas.enums.PetStatus;
import org.example.tas.models.petstore.Pet;

import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static java.lang.Thread.sleep;

public class petHelper {
    private static final int READ_ATTEMPTS = 5;

    public static Pet createNewPet(String name, String status) {
        Pet pet = new Pet();
        pet.setId(ThreadLocalRandom.current().nextLong(9_000_000_000L, 9_999_999_999L));
        pet.setCategory(new Pet.Category());
        pet.setName(name);
        pet.setStatus(PetStatus.fromValue(status).value());
        return pet;
    }

    public Response getPetExpecting(Pet pet, int status) {
        Response result = null;
        for (int attempt = 1; attempt <= READ_ATTEMPTS; attempt++) {
            result = given(spec).get("/pet/{id}", pet.id());
            if (result.statusCode() == status) {
                break;
            }
            sleep();
        }
        return result;
    }
}

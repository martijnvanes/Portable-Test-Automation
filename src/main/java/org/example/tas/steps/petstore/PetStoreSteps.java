package org.example.tas.steps.petstore;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.example.tas.enums.PetStatus;
import org.example.tas.models.Pet;
import org.example.tas.run.TestTargets;
import org.example.tas.util.validation.ResultValidator;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;

public class PetStoreSteps {

    /** The public Petstore is load balanced; a write is not always visible on the next read. */
    private static final int READ_ATTEMPTS = 5;
    private static final long READ_DELAY_MS = 500;

    private RequestSpecification spec;
    private ResultValidator validator;
    private Pet pet;
    private Response response;
    private Pet retrievedPet;

    @Before("@petstore")
    public void setUp(Scenario scenario) {
        spec = new RequestSpecBuilder()
                .setBaseUri(TestTargets.current().petstore().baseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
        // Properties validated below "exact" end up in the report of the scenario
        validator = new ResultValidator(scenario::log);
    }

    @Given("a new pet named {string} with status {string}")
    public void aNewPet(String name, String status) {
        // A large random id keeps us clear of pets other users created on the shared server
        long id = ThreadLocalRandom.current().nextLong(9_000_000_000L, 9_999_999_999L);
        pet = new Pet(id, null, name, List.of(), List.of(), PetStatus.fromValue(status));
    }

    @Given("a pet named {string} exists in the store")
    public void aPetExists(String name) {
        aNewPet(name, PetStatus.AVAILABLE.value());
        addPet();
        responseStatusIs(200);
        petCanBeRetrieved();
    }

    @When("I add the pet to the store")
    public void addPet() {
        response = given(spec).body(pet).post("/pet");
    }

    @When("I delete the pet")
    public void deletePet() {
        response = given(spec).delete("/pet/{id}", pet.id());
    }

    @Then("the response status is {int}")
    public void responseStatusIs(int status) {
        validator.validate(status, response.statusCode());
    }

    @Then("the pet can be retrieved by its id")
    public void petCanBeRetrieved() {
        response = getPetExpecting(200);
        responseStatusIs(200);
        retrievedPet = response.as(Pet.class);
        validator.validate(pet.id(), retrievedPet.id());
    }

    @Then("the retrieved pet has name {string} and status {string}")
    public void retrievedPetHas(String name, String status) {
        // The pet is fully predictable, so every property is validated exactly
        Pet expected = new Pet(pet.id(), pet.category(), name, pet.photoUrls(), pet.tags(),
                PetStatus.fromValue(status));
        validator.validate(expected, retrievedPet);
    }

    @Then("the pet can no longer be found")
    public void petIsGone() {
        response = getPetExpecting(404);
        responseStatusIs(404);
    }

    private Response getPetExpecting(int status) {
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

    private static void sleep() {
        try {
            Thread.sleep(READ_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}

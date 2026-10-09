package org.example.tas.steps.petstore;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.example.tas.run.TestTargets;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class PetStoreSteps {

    /** The public Petstore is load balanced; a write is not always visible on the next read. */
    private static final int READ_ATTEMPTS = 5;
    private static final long READ_DELAY_MS = 500;

    private RequestSpecification spec;
    private Map<String, Object> pet;
    private Response response;

    @Before("@petstore")
    public void setUp() {
        spec = new RequestSpecBuilder()
                .setBaseUri(TestTargets.current().petstore().baseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    @Given("a new pet named {string} with status {string}")
    public void aNewPet(String name, String status) {
        // A large random id keeps us clear of pets other users created on the shared server
        long id = ThreadLocalRandom.current().nextLong(9_000_000_000L, 9_999_999_999L);
        pet = Map.of("id", id, "name", name, "status", status);
    }

    @Given("a pet named {string} exists in the store")
    public void aPetExists(String name) {
        aNewPet(name, "available");
        addPet();
        response.then().statusCode(200);
        petCanBeRetrieved();
    }

    @When("I add the pet to the store")
    public void addPet() {
        response = given(spec).body(pet).post("/pet");
    }

    @When("I delete the pet")
    public void deletePet() {
        response = given(spec).delete("/pet/{id}", petId());
    }

    @Then("the response status is {int}")
    public void responseStatusIs(int status) {
        response.then().statusCode(status);
    }

    @Then("the pet can be retrieved by its id")
    public void petCanBeRetrieved() {
        response = getPetExpecting(200);
        response.then().statusCode(200).body("id", equalTo(petId()));
    }

    @Then("the retrieved pet has name {string} and status {string}")
    public void retrievedPetHas(String name, String status) {
        response.then()
                .body("name", equalTo(name))
                .body("status", equalTo(status));
    }

    @Then("the pet can no longer be found")
    public void petIsGone() {
        getPetExpecting(404).then().statusCode(404);
    }

    private Response getPetExpecting(int status) {
        Response result = null;
        for (int attempt = 1; attempt <= READ_ATTEMPTS; attempt++) {
            result = given(spec).get("/pet/{id}", petId());
            if (result.statusCode() == status) {
                break;
            }
            sleep();
        }
        return result;
    }

    private long petId() {
        return (Long) pet.get("id");
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

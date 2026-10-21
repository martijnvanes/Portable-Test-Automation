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
import org.example.tas.models.petstore.Pet;
import org.example.tas.run.TestTargets;
import org.example.tas.util.helpers.petstore.petHelper;
import org.example.tas.util.validation.ResultValidator;


import static io.restassured.RestAssured.given;
import static org.example.tas.util.messaging.petstore.deletePet;

public class PetStoreSteps {

    /** The public Petstore is load balanced; a write is not always visible on the next read. */

    private static final long READ_DELAY_MS = 500;

    private RequestSpecification spec;
    private ResultValidator validator;
    private Pet pet;
    private Response response;
    private Pet retrievedPet;



    @Given("a new pet named {string} with status {string}")
    public void aNewPet(String name, String status) {
        // A large random id keeps us clear of pets other users created on the shared server
        Pet pet = petHelper.createNewPet(name, status);
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
    public void iDeleteThePeletePet() {
        Pet pet = new Pet();
        response = deletePet(pet);
    }

    @Then("the response status is {int}")
    public void responseStatusIs(int status) {
        validator.validate(status, response.statusCode());
    }

    @Then("the pet can be retrieved by its id")
    public void petCanBeRetrieved() {
        Pet pet = new Pet();
        response = petHelper.getPetExpecting(pet,200);
        responseStatusIs(200);
        retrievedPet = response.as(Pet.class);
        validator.validate(pet.id(), retrievedPet.id());
    }

    @Then("the retrieved pet has name {string} and status {string}")
    public void retrievedPetHas(String name, String status) {
        // The pet is fully predictable, so every property is validated exactly
        Pet expected = new Pet();
        Pet actual = retrievedPet;
        validator.validate(expected, actual);
    }

    @Then("the pet can no longer be found")
    public void petIsGone() {
        response = getPetExpecting(404);
        responseStatusIs(404);
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

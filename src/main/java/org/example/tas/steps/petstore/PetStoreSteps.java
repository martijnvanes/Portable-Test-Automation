package org.example.tas.steps.petstore;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.example.tas.enums.PetStatus;
import org.example.tas.models.petstore.Pet;
import org.example.tas.util.BaseTest;
import org.example.tas.util.messaging.MessagingPetstore;
import org.example.tas.util.scenario.CucumberScenario;
import org.example.tas.util.scenario.ScenarioContext;


import static io.restassured.RestAssured.given;
import static org.example.tas.config.BaseConfiguration.READ_DELAY_MS;
import static org.example.tas.util.helpers.petstore.petHelper.createNewPet;
import static org.example.tas.util.messaging.MessagingPetstore.deletePet;

public class PetStoreSteps extends CucumberScenario {

    private final ScenarioContext scenarioContext;

    public PetStoreSteps(BaseTest baseTest) {
        super(baseTest);
        scenarioContext = baseTest.scenarioContext;
    }

    /**
     * The public Petstore is load balanced; a write is not always visible on the next read.
     */
    private Response response;

    @Given("a new pet named {string} with status {string}")
    public void aNewPet(String name, String status) {
        // A large random id keeps us clear of pets other users created on the shared server
        scenarioContext.setPet(createNewPet(name, status));
    }

    @Given("a pet named {string} exists in the store")
    public void aPetExists(String name) {
        Pet pet = createNewPet(name, PetStatus.AVAILABLE.value());
        scenarioContext.setPetToDelete(MessagingPetstore.postPet(pet).as(Pet.class));
    }

    @When("I add the pet to the store")
    public void iAddPetThePetToTheStore() {
        Pet pet = scenarioContext.getPet();
        scenarioContext.setPetLifecycleResponse(MessagingPetstore.postPet(pet));
        sleep();
    }

    @When("I delete the pet")
    public void iDeleteThePeletePet() {
        scenarioContext.setPetLifecycleResponse(deletePet(scenarioContext.getPetToDelete()));
        sleep();
    }

    @Then("the response status is {int}")
    public void responseStatusIs(int responseCode) {
        baseTest.petStoreValidation.validateResponseStatus(responseCode, scenarioContext.getPetLifecycleResponse().getStatusCode());
    }

    @Then("the pet can be retrieved by its id")
    public void petCanBeRetrieved() {
        Pet postedPet = scenarioContext.getPet();
        response = MessagingPetstore.getPet(postedPet);
        scenarioContext.setActualPet(response.getBody().as(Pet.class));
    }

    @Then("the retrieved pet has name {string} and status {string}")
    public void retrievedPetHas(String name, String status) {
        Pet expected = new Pet();
        expected.setName(name);
        expected.setStatus(status);
        Pet actual = scenarioContext.getActualPet();
        baseTest.petStoreValidation.validateStoredPet(expected, actual);
    }

    @Then("the pet can no longer be found")
    public void thePetCanNoLongerBeFound() {
        response = MessagingPetstore.getPet(scenarioContext.getPetToDelete());
        baseTest.petStoreValidation.validateResponseStatus(404, response.getStatusCode());
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

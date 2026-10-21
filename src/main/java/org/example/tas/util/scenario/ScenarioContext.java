package org.example.tas.util.scenario;

import io.restassured.response.Response;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.tas.models.petstore.Pet;

@Data
@Slf4j
public class ScenarioContext {
    private Pet pet;
    private Response petLifecycleResponse;
    private Pet actualPet;
    private Pet petToDelete;
}

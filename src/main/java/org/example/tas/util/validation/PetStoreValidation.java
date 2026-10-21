package org.example.tas.util.validation;

import org.example.tas.models.petstore.Pet;

import static org.example.tas.util.helpers.AssertionHelper.*;

public class PetStoreValidation {
    public void validateStoredPet(Pet expectedPet, Pet actualPet) {
        String[] ignoreList = new String[]{"category", "id", "photoUrls", "tags"};
        softlyAssertIsEqualToIgnoringFields(actualPet, expectedPet, "Validating Pet", "Validating Pet failed", ignoreList);
        assertAll();
    }

    public void validateResponseStatus(int expected, int actual) {
        softlyAssertIsEqualTo(actual, expected, "Verifying the status code is as expected", "Status code not as expected. Expeceted status code " + expected + " but actual status code " + actual);
        assertAll();
    }
}

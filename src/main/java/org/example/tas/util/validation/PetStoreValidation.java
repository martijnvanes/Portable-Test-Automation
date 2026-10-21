package org.example.tas.util.validation;

import org.example.tas.models.petstore.Pet;

import static org.example.tas.util.helpers.AssertionHelper.softlyAssertIsEqualToIgnoringFields;

public class PetStoreValidation {
    public void validateStoredPet(Pet expectedPet, Pet actualPet) {
        softlyAssertIsEqualToIgnoringFields(actualPet, expectedPet, "Validating Pet", "Validating Pet failed", "ignoreList");
    }
}

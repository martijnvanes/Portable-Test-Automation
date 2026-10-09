@petstore
Feature: Pets in the Swagger Petstore
  The Petstore lets clients add, look up and remove pets.

  Scenario: Add a new pet and retrieve it
    Given a new pet named "Rex" with status "available"
    When I add the pet to the store
    Then the response status is 200
    And the pet can be retrieved by its id
    And the retrieved pet has name "Rex" and status "available"

  Scenario: Delete a pet
    Given a pet named "Bello" exists in the store
    When I delete the pet
    Then the response status is 200
    And the pet can no longer be found

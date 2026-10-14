package org.example.tas.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.tas.enums.PetStatus;

import java.util.List;

/**
 * A pet as the Petstore API sends and returns it. All properties of the API are modelled on
 * purpose (no ignore-unknown), so a property that appears in a response but not here fails the
 * deserialization instead of silently escaping validation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Pet(Long id, Category category, String name, List<String> photoUrls, List<Tag> tags,
                  PetStatus status) {
}

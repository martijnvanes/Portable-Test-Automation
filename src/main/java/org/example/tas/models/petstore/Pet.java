package org.example.tas.models.petstore;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * A pet as the Petstore API sends and returns it. All properties of the API are modelled on
 * purpose (no ignore-unknown), so a property that appears in a response but not here fails the
 * deserialization instead of silently escaping validation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Pet {
    private Long id;
    private Category category;
    private String name;
    private List<String> photoUrls;
    private List<Tag> tags;
    private String status;

    @Data
    public static class Category {
        private Long id;
        private String name;
    }

    @Data
    public static class Tag {
        private Long id;
        private String name;
    }
}
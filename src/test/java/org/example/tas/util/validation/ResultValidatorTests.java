package org.example.tas.util.validation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultValidatorTests {

    record Category(Long id, String name) { }

    record Pet(Long id, String name, String status, Category category, List<String> tags) { }

    private final ResultValidator validator = new ResultValidator();

    private static Pet pet(long id, String name) {
        return new Pet(id, name, "available", null, List.of());
    }

    private static boolean sameId(Pet expected, Pet actual) {
        return expected.id().equals(actual.id());
    }

    // --- single object -------------------------------------------------------------------

    @Test
    void equalObjectsPass() {
        assertThatCode(() -> validator.validate(pet(1, "Rex"), pet(1, "Rex"))).doesNotThrowAnyException();
    }

    @Test
    void mismatchNamesTheProperty() {
        assertThatThrownBy(() -> validator.validate(pet(1, "Rex"), pet(1, "Fido")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Pet.name")
                .hasMessageContaining("Rex")
                .hasMessageContaining("Fido");
    }

    @Test
    void allMismatchingPropertiesAreReportedTogether() {
        Pet expected = new Pet(1L, "Rex", "available", null, List.of());
        Pet actual = new Pet(2L, "Fido", "sold", null, List.of());
        assertThatThrownBy(() -> validator.validate(expected, actual))
                .hasMessageContaining("Pet.id")
                .hasMessageContaining("Pet.name")
                .hasMessageContaining("Pet.status");
    }

    @Test
    void nullInTheExpectedMeansTheActualMustBeNull() {
        Pet expected = pet(1, "Rex");
        Pet actual = new Pet(1L, "Rex", "available", new Category(1L, "dogs"), List.of());
        assertThatThrownBy(() -> validator.validate(expected, actual))
                .hasMessageContaining("Pet.category");
    }

    @Test
    void nestedRecordsAreComparedPerProperty() {
        Pet expected = new Pet(1L, "Rex", "available", new Category(1L, "dogs"), List.of());
        Pet actual = new Pet(1L, "Rex", "available", new Category(1L, "cats"), List.of());
        assertThatThrownBy(() -> validator.validate(expected, actual))
                .hasMessageContaining("Pet.category.name");
    }

    @Test
    void nullActualFailsClearlyInsteadOfThrowingNpe() {
        assertThatThrownBy(() -> validator.validate(pet(1, "Rex"), null))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("actual is null");
    }

    // --- lists ---------------------------------------------------------------------------

    @Test
    void matchingListsPassInAnyOrder() {
        List<Pet> expected = List.of(pet(1, "Rex"), pet(2, "Fido"));
        List<Pet> actual = List.of(pet(2, "Fido"), pet(1, "Rex"));
        assertThatCode(() -> validator.validateAll(expected, actual, ResultValidatorTests::sameId))
                .doesNotThrowAnyException();
    }

    @Test
    void matchedActualIsValidatedPropertyByProperty() {
        assertThatThrownBy(() -> validator.validateAll(
                List.of(pet(1, "Rex")), List.of(pet(1, "Fido")), ResultValidatorTests::sameId))
                .hasMessageContaining("Pet[0].name");
    }

    @Test
    void expectedWithoutActualFails() {
        assertThatThrownBy(() -> validator.validateAll(
                List.of(pet(1, "Rex"), pet(2, "Fido")), List.of(pet(1, "Rex")), ResultValidatorTests::sameId))
                .hasMessageContaining("Pet[1] has no matching actual");
    }

    @Test
    void actualWithoutExpectedFails() {
        assertThatThrownBy(() -> validator.validateAll(
                List.of(pet(1, "Rex")), List.of(pet(1, "Rex"), pet(2, "Fido")), ResultValidatorTests::sameId))
                .hasMessageContaining("actuals without an expected")
                .hasMessageContaining("Fido");
    }

    @Test
    void differingCandidatesAreAmbiguous() {
        List<Pet> actual = List.of(pet(1, "Rex"), pet(2, "Fido"));
        assertThatThrownBy(() -> validator.validateAll(
                List.of(pet(1, "Rex")), actual, (e, a) -> a.status().equals("available")))
                .hasMessageContaining("matches 2 different actuals");
    }

    @Test
    void identicalCandidatesPassWhenEachHasAnExpected() {
        List<Pet> expected = List.of(pet(1, "Rex"), pet(1, "Rex"));
        List<Pet> actual = List.of(pet(1, "Rex"), pet(1, "Rex"));
        assertThatCode(() -> validator.validateAll(expected, actual, ResultValidatorTests::sameId))
                .doesNotThrowAnyException();
    }

    @Test
    void identicalActualsBeyondTheExpectedCountAreLeftOver() {
        assertThatThrownBy(() -> validator.validateAll(
                List.of(pet(1, "Rex")), List.of(pet(1, "Rex"), pet(1, "Rex")), ResultValidatorTests::sameId))
                .hasMessageContaining("actuals without an expected");
    }

    @Test
    void identicalExpectedsBeyondTheActualCountFail() {
        assertThatThrownBy(() -> validator.validateAll(
                List.of(pet(1, "Rex"), pet(1, "Rex")), List.of(pet(1, "Rex")), ResultValidatorTests::sameId))
                .hasMessageContaining("Pet[1] has no matching actual");
    }

    @Test
    void everyKindOfFailureLandsInOneReport() {
        List<Pet> expected = List.of(pet(1, "Rex"), pet(2, "Fido"));
        List<Pet> actual = List.of(pet(1, "Wrong"), pet(3, "Extra"));
        assertThatThrownBy(() -> validator.validateAll(expected, actual, ResultValidatorTests::sameId))
                .hasMessageContaining("Pet[0].name")
                .hasMessageContaining("Pet[1] has no matching actual")
                .hasMessageContaining("actuals without an expected");
    }

    @Test
    void theListsPassedInAreNotChanged() {
        List<Pet> expected = new ArrayList<>(List.of(pet(1, "Rex")));
        List<Pet> actual = new ArrayList<>(List.of(pet(1, "Rex")));
        validator.validateAll(expected, actual, ResultValidatorTests::sameId);
        assertThat(expected).hasSize(1);
        assertThat(actual).hasSize(1);
    }

    @Test
    void emptyListsPass() {
        assertThatCode(() -> validator.validateAll(List.<Pet>of(), List.<Pet>of(), ResultValidatorTests::sameId))
                .doesNotThrowAnyException();
    }

    // --- property rules ------------------------------------------------------------------

    @Test
    void ignoredPropertyIsNotCompared() {
        PropertyRules rules = PropertyRules.none().rule("id", PropertyRule.ignore("generated by the server"));
        assertThatCode(() -> validator.validate(pet(1, "Rex"), pet(2, "Rex"), rules)).doesNotThrowAnyException();
    }

    @Test
    void ignoreNeedsAReason() {
        assertThatThrownBy(() -> PropertyRule.ignore(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void regexRuleMustMatchTheWholeValue() {
        PropertyRules rules = PropertyRules.none().rule("name", PropertyRule.regex("Rex-\\d+"));
        assertThatCode(() -> validator.validate(pet(1, "x"), pet(1, "Rex-42"), rules)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(pet(1, "x"), pet(1, "Rex-42-extra"), rules))
                .hasMessageContaining("Pet.name");
    }

    @Test
    void partialRulesCheckThePart() {
        PropertyRules rules = PropertyRules.none().rule("name", PropertyRule.contains("ex"));
        assertThatCode(() -> validator.validate(pet(1, "x"), pet(1, "Rex"), rules)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(pet(1, "x"), pet(1, "Fido"), rules))
                .hasMessageContaining("Pet.name");
    }

    @Test
    void notEmptyRejectsNullBlankStringsAndEmptyCollections() {
        PropertyRules nameRule = PropertyRules.none().rule("name", PropertyRule.notEmpty());
        assertThatCode(() -> validator.validate(pet(1, "x"), pet(1, "Rex"), nameRule)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(pet(1, "x"), pet(1, ""), nameRule))
                .hasMessageContaining("Pet.name must not be empty or null");

        PropertyRules tagsRule = PropertyRules.none().rule("tags", PropertyRule.notEmpty());
        assertThatThrownBy(() -> validator.validate(pet(1, "Rex"), pet(1, "Rex"), tagsRule))
                .hasMessageContaining("Pet.tags must not be empty or null");
    }

    @Test
    void emptyRulePassesOnNullAndEmpty() {
        PropertyRules rules = PropertyRules.none().rule("category", PropertyRule.empty());
        assertThatCode(() -> validator.validate(pet(1, "Rex"), pet(1, "Rex"), rules)).doesNotThrowAnyException();
    }

    @Test
    void rulesWorkOnNestedPropertiesByPath() {
        Pet expected = new Pet(1L, "Rex", "available", new Category(1L, "x"), List.of());
        Pet actual = new Pet(1L, "Rex", "available", new Category(1L, "dogs"), List.of());
        PropertyRules rules = PropertyRules.none().rule("category.name", PropertyRule.ignore("not predictable"));
        assertThatCode(() -> validator.validate(expected, actual, rules)).doesNotThrowAnyException();
    }

    @Test
    void ruleForAnUnknownPropertyFailsInsteadOfSilentlyNotApplying() {
        PropertyRules rules = PropertyRules.none().rule("nmae", PropertyRule.ignore("typo"));
        assertThatThrownBy(() -> validator.validate(pet(1, "Rex"), pet(1, "Rex"), rules))
                .hasMessageContaining("Rule for property 'nmae' was never applied");
    }

    @Test
    void weakerChecksAreReportedAsNotes() {
        List<String> notes = new ArrayList<>();
        ResultValidator noting = new ResultValidator(notes::add);
        PropertyRules rules = PropertyRules.none()
                .rule("id", PropertyRule.ignore("generated by the server"))
                .rule("name", PropertyRule.regex("Rex.*"));
        noting.validateAll(List.of(pet(1, "Rex"), pet(2, "Rex2")), List.of(pet(3, "Rex"), pet(4, "Rex2")),
                (e, a) -> e.status().equals(a.status()) && a.name().startsWith(e.name()) && e.name().length() == a.name().length(),
                rules);
        assertThat(notes).containsExactly("id ignored: generated by the server", "name matches regex Rex.*");
    }
}

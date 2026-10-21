package org.example.tas.util.helpers;

import org.assertj.core.api.SoftAssertions;
import org.example.tas.models.petstore.Pet;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.example.tas.util.helpers.CucumberReportLogger.logForCucumberReport;

public class AssertionHelper {
    private static final ThreadLocal<SoftAssertions> softly = ThreadLocal.withInitial(SoftAssertions::new);

    private AssertionHelper() {
        // static utility class
    }

    /***
     * Resets the soft assertions for the current thread.
     * Call this at start of each scenario to ensure a clean state.
     */
    public static void reset() {
        softly.set(new SoftAssertions());
    }

    /***
     * Checks all previous soft assertions and makes them 'hard'.
     * Test will stop if there are one or more failed assertions.
     */
    public static void assertAll() {
        logForCucumberReport("Asserting all SoftAssertions done so far ... \n will stop test if there are failed assertions!");
        softly.get().assertAll();
    }

    /***
     * Soft assertion: isEqualTo (3-param convenience overload).
     *
     * @param actual the actual object
     * @param expected the expected object
     * @param field field used for (error) logging.
     */
    public static void softlyAssertIsEqualTo(Object actual, Object expected, String field) {
        logForCucumberReport("Verifying " + field + " is " + expected);
        softly.get().assertThat(actual).as("Incorrect" + field).isEqualTo(expected);
    }

    /***
     * Soft assertion: isEqualTo (4-param convenience overload).
     *
     * @param actual the actual object
     * @param expected the expected object
     * @param logMessage field used for logging.
     * @param failureMessage field used for logging if assertion failes.
     */
    public static void softlyAssertIsEqualTo(Object actual, Object expected, String logMessage, String failureMessage) {
        if (!Objects.isNull(logMessage)) {
            logForCucumberReport(logMessage);
        }
        softly.get().assertThat(actual).as(failureMessage).isEqualTo(expected);
    }

    public static void softlyAssertIsEqualToIgnoringFields(Object actual, Object expected, String logMessage, String failureMessage, String... ignoreList) {
        if (!Objects.isNull(logMessage)) {
            logForCucumberReport(logMessage);
        }
        Set<String> ignoredFields = new HashSet<>(Arrays.asList(ignoreList));
        Field[] fields = expected.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (ignoredFields.contains(field.getName())) {
                logForCucumberReport("Ignoring field: " + field.getName());
                continue;
            }
            field.setAccessible(true);
            try {
                Object expectedValue = field.get(expected);
                Object actualValue = field.get(actual);
                logForCucumberReport(field.getName() + " - Expected: " + expectedValue + " | Actual: " + actualValue);
                field.set(actual, expected);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                logForCucumberReport(field.getName() + " - Could not read field with exception: \n" + e.getMessage());
            }
        }
        softly.get().assertThat(actual).as(failureMessage).usingRecursiveComparison()
                .ignoringFields(ignoreList)
                .isEqualTo(expected);
    }
}

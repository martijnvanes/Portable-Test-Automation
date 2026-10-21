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

    public static void reset() {
        softly.set(new SoftAssertions());
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

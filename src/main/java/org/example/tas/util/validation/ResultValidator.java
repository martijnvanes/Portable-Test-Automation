package org.example.tas.util.validation;

import org.assertj.core.api.SoftAssertions;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Compares expected results with actual results of the same type.
 * <p>
 * Everything is soft asserted into one report: unmatched expecteds, ambiguous matches, property
 * mismatches and actuals that no expected claimed. A single {@code assertAll()} at the end throws
 * one {@link AssertionError} containing all of them, so one run shows as many failures as possible.
 * <p>
 * Models are compared property by property when they are records; nested records are compared
 * recursively. Anything else is compared with {@code equals}.
 */
public class ResultValidator {

    private final Consumer<String> noteSink;

    public ResultValidator() {
        this(note -> { });
    }

    /**
     * @param noteSink receives one note for every property that is validated below "exact", so
     *                 weak checks stay visible, for example {@code scenario::log}
     */
    public ResultValidator(Consumer<String> noteSink) {
        this.noteSink = Objects.requireNonNull(noteSink, "noteSink");
    }

    public <T> void validate(T expected, T actual) {
        validate(expected, actual, PropertyRules.none());
    }

    public <T> void validate(T expected, T actual, PropertyRules rules) {
        Objects.requireNonNull(expected, "expected");
        Run run = new Run(rules);
        compareItem(run, expected.getClass().getSimpleName(), expected, actual);
        run.finish();
    }

    /**
     * Validates every expected against its matching actual, ignoring order.
     * <p>
     * For each expected the {@code matcher} selects the candidate actuals. No candidate fails;
     * candidates that differ from each other fail as ambiguous (tighten the matcher); identical
     * candidates are fine and exactly one of them is consumed. Actuals still left at the end had
     * no expected and fail the validation. The lists passed in are not changed.
     *
     * @param matcher decides per validation which actual corresponds to an expected
     */
    public <T> void validateAll(List<? extends T> expecteds, List<? extends T> actuals,
                                BiPredicate<? super T, ? super T> matcher) {
        validateAll(expecteds, actuals, matcher, PropertyRules.none());
    }

    public <T> void validateAll(List<? extends T> expecteds, List<? extends T> actuals,
                                BiPredicate<? super T, ? super T> matcher, PropertyRules rules) {
        Objects.requireNonNull(expecteds, "expecteds");
        Objects.requireNonNull(actuals, "actuals");
        Objects.requireNonNull(matcher, "matcher");
        Run run = new Run(rules);
        List<T> remaining = new ArrayList<>(actuals);
        for (int i = 0; i < expecteds.size(); i++) {
            validateExpected(run, i, expecteds.get(i), remaining, matcher);
        }
        run.soft.assertThat(remaining).as("actuals without an expected").isEmpty();
        run.finish();
    }

    private <T> void validateExpected(Run run, int index, T expected, List<T> remaining,
                                      BiPredicate<? super T, ? super T> matcher) {
        String label = expected.getClass().getSimpleName() + "[" + index + "]";
        List<T> candidates = remaining.stream().filter(actual -> matcher.test(expected, actual)).toList();
        if (candidates.isEmpty()) {
            run.soft.fail("%s has no matching actual: %s", label, expected);
        } else if (candidates.stream().distinct().count() > 1) {
            run.soft.fail("%s matches %d different actuals, tighten the match criteria: %s",
                    label, candidates.size(), expected);
        } else {
            T actual = candidates.get(0);
            compareItem(run, label, expected, actual);
            remaining.remove(actual);
        }
    }

    private void compareItem(Run run, String label, Object expected, Object actual) {
        if (actual == null) {
            run.soft.fail("%s: actual is null, expected %s", label, expected);
            return;
        }
        compare(run, label, "", expected, actual);
    }

    private void compare(Run run, String label, String path, Object expected, Object actual) {
        PropertyRule rule = run.rules.forPath(path);
        if (rule != null) {
            if (run.applied.add(path)) {
                noteSink.accept(path + " " + rule.description());
            }
            rule.check().apply(run.soft, label, actual);
        } else if (expected != null && actual != null && expected.getClass().isRecord()
                && expected.getClass() == actual.getClass()) {
            for (RecordComponent component : expected.getClass().getRecordComponents()) {
                String name = component.getName();
                compare(run, label + "." + name, path.isEmpty() ? name : path + "." + name,
                        read(component, expected), read(component, actual));
            }
        } else {
            run.soft.assertThat(actual).as(label).isEqualTo(expected);
        }
    }

    private static Object read(RecordComponent component, Object record) {
        try {
            Method accessor = component.getAccessor();
            accessor.setAccessible(true);
            return accessor.invoke(record);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read " + component.getName() + " of " + record.getClass(), e);
        }
    }

    /** State of one validation: the soft assertions, its rules and which rules were used. */
    private static final class Run {
        final SoftAssertions soft = new SoftAssertions();
        final PropertyRules rules;
        final Set<String> applied = new HashSet<>();

        Run(PropertyRules rules) {
            this.rules = Objects.requireNonNull(rules, "rules");
        }

        /** A rule for a property that was never validated is probably a typo, and would silently not apply. */
        void finish() {
            for (String path : rules.paths()) {
                if (!applied.contains(path)) {
                    soft.fail("Rule for property '%s' was never applied, check the property path", path);
                }
            }
            soft.assertAll();
        }
    }
}

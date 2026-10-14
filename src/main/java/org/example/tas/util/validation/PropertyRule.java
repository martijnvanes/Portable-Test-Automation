package org.example.tas.util.validation;

import org.assertj.core.api.SoftAssertions;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A weaker-than-exact check for one property, used when the actual value cannot be fully predicted.
 * <p>
 * Strength, strongest first: exact value (the default, needs no rule), partial value
 * ({@link #contains}, {@link #startsWith}, {@link #endsWith}), {@link #regex}, {@link #notEmpty}
 * / {@link #empty}, and {@link #ignore} as the last resort. Only go down a level when the
 * level above is impossible.
 */
public record PropertyRule(String description, Check check) {

    @FunctionalInterface
    public interface Check {
        void apply(SoftAssertions soft, String label, Object actual);
    }

    public static PropertyRule contains(String part) {
        return new PropertyRule("contains \"" + part + "\"",
                (soft, label, actual) -> soft.assertThat(text(actual)).as(label).contains(part));
    }

    public static PropertyRule startsWith(String prefix) {
        return new PropertyRule("starts with \"" + prefix + "\"",
                (soft, label, actual) -> soft.assertThat(text(actual)).as(label).startsWith(prefix));
    }

    public static PropertyRule endsWith(String suffix) {
        return new PropertyRule("ends with \"" + suffix + "\"",
                (soft, label, actual) -> soft.assertThat(text(actual)).as(label).endsWith(suffix));
    }

    /** The whole value must match the regular expression. */
    public static PropertyRule regex(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return new PropertyRule("matches regex " + regex,
                (soft, label, actual) -> soft.assertThat(text(actual)).as(label).matches(pattern));
    }

    /** Fails on null, an empty string, an empty collection, map or array. */
    public static PropertyRule notEmpty() {
        return new PropertyRule("is not empty",
                (soft, label, actual) -> soft.assertThat(isEmpty(actual))
                        .as("%s must not be empty or null", label).isFalse());
    }

    /** Passes on null, an empty string, an empty collection, map or array. */
    public static PropertyRule empty() {
        return new PropertyRule("is empty or null",
                (soft, label, actual) -> soft.assertThat(isEmpty(actual))
                        .as("%s must be empty or null", label).isTrue());
    }

    /** Skips the property. The reason is mandatory and shows up in the validation notes. */
    public static PropertyRule ignore(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Ignoring a property needs a reason");
        }
        return new PropertyRule("ignored: " + reason, (soft, label, actual) -> { });
    }

    public PropertyRule {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(check, "check");
    }

    private static String text(Object actual) {
        return actual == null ? null : actual.toString();
    }

    private static boolean isEmpty(Object actual) {
        return switch (actual) {
            case null -> true;
            case CharSequence chars -> chars.isEmpty();
            case Collection<?> collection -> collection.isEmpty();
            case Map<?, ?> map -> map.isEmpty();
            default -> actual.getClass().isArray() && Array.getLength(actual) == 0;
        };
    }
}

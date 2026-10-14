package org.example.tas.util.validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Per-property rules for one validation, keyed by property path ({@code "email"}, or
 * {@code "category.name"} for a nested record). Every property without a rule is compared exactly.
 */
public final class PropertyRules {

    private final Map<String, PropertyRule> rules = new LinkedHashMap<>();

    public static PropertyRules none() {
        return new PropertyRules();
    }

    public PropertyRules rule(String path, PropertyRule rule) {
        rules.put(Objects.requireNonNull(path, "path"), Objects.requireNonNull(rule, "rule"));
        return this;
    }

    PropertyRule forPath(String path) {
        return rules.get(path);
    }

    Set<String> paths() {
        return rules.keySet();
    }
}

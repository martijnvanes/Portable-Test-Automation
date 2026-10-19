package org.example.tas.run;

import org.example.tas.config.TasProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

/**
 * Hands the configured target URLs to the step definitions.
 * Cucumber creates the step classes itself, so they cannot receive Spring beans;
 * {@link TestRunService} sets this before each run (runs are serialized); without it the values
 * come from application.yml, see {@link #current()}.
 */
public final class TestTargets {

    private static volatile TasProperties.Targets current;

    private TestTargets() {
    }

    static void set(TasProperties.Targets targets) {
        current = targets;
    }

    static void clear() {
        current = null;
    }

    public static TasProperties.Targets current() {
        if (current == null) {
            current = loadFromApplicationYml();
        }
        return current;
    }

    /**
     * Fallback for runs that bypass {@link TestRunService}, such as a single scenario started from the IDE.
     * Binds {@code tas.targets} from application.yml without a Spring context; the environment variables
     * (PETSTORE_BASE_URL and so on) still override the defaults, exactly as in a normal run.
     */
    private static synchronized TasProperties.Targets loadFromApplicationYml() {
        if (current != null) {
            return current;
        }
        try {
            StandardEnvironment environment = new StandardEnvironment();
            List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                    .load("application", new ClassPathResource("application.yml"));
            sources.forEach(environment.getPropertySources()::addLast);
            return Binder.get(environment).bind("tas.targets", TasProperties.Targets.class)
                    .orElseThrow(() -> new IllegalStateException("No tas.targets found in application.yml"));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read application.yml for the test targets", e);
        }
    }
}

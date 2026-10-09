package org.example.tas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** Serves the generated reports under {@code /reports/**}. */
@Configuration
public class ReportsWebConfig implements WebMvcConfigurer {

    private final TasProperties properties;

    public ReportsWebConfig(TasProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.reportsDir()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/reports/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}

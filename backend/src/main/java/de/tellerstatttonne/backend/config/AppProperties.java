package de.tellerstatttonne.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Frontend frontend) {

    public record Frontend(String baseUrl) {}
}

package de.tellerstatttonne.backend.feature;

public record FeatureRequest(
    String key,
    String label,
    String description,
    String category,
    Integer sortOrder
) {}

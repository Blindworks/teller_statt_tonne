package de.tellerstatttonne.backend.feature;

public record Feature(
    Long id,
    String key,
    String label,
    String description,
    String category,
    int sortOrder
) {}

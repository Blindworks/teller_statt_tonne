package de.tellerstatttonne.backend.partnercategory;

public record PartnerCategory(
    Long id,
    String code,
    String label,
    String icon,
    int orderIndex,
    boolean active
) {}

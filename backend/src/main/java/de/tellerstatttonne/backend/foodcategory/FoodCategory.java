package de.tellerstatttonne.backend.foodcategory;

public record FoodCategory(
    Long id,
    String name,
    String emoji,
    String colorHex,
    int sortOrder,
    boolean active
) {}

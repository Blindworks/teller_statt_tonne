package de.tellerstatttonne.backend.feature;

final class FeatureMapper {

    private FeatureMapper() {}

    static Feature toDto(FeatureEntity e) {
        return new Feature(
            e.getId(),
            e.getKey(),
            e.getLabel(),
            e.getDescription(),
            e.getCategory(),
            e.getSortOrder()
        );
    }
}

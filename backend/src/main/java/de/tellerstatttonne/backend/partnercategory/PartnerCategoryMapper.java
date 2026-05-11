package de.tellerstatttonne.backend.partnercategory;

final class PartnerCategoryMapper {

    private PartnerCategoryMapper() {}

    static PartnerCategory toDto(PartnerCategoryEntity e) {
        return new PartnerCategory(
            e.getId(),
            e.getCode(),
            e.getLabel(),
            e.getIcon(),
            e.getOrderIndex(),
            e.isActive()
        );
    }

    static void applyToEntity(PartnerCategoryEntity target, PartnerCategory src) {
        target.setCode(src.code() == null ? null : src.code().trim());
        target.setLabel(src.label() == null ? null : src.label().trim());
        target.setIcon(src.icon() == null ? null : src.icon().trim());
        target.setOrderIndex(src.orderIndex());
        target.setActive(src.active());
    }
}

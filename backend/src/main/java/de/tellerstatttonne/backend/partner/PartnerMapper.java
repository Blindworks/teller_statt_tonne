package de.tellerstatttonne.backend.partner;

import java.util.List;

final class PartnerMapper {

    private PartnerMapper() {}

    static Partner toDto(PartnerEntity e) {
        return new Partner(
            e.getId(),
            e.getName(),
            e.getCategoryId(),
            e.getStreet(),
            e.getPostalCode(),
            e.getCity(),
            e.getLogoUrl(),
            e.getContact() == null
                ? new Partner.Contact(null, null, null)
                : new Partner.Contact(e.getContact().getName(), e.getContact().getEmail(), e.getContact().getPhone()),
            e.getPickupSlots() == null
                ? List.of()
                : e.getPickupSlots().stream()
                    .map(s -> new Partner.PickupSlot(
                        s.getWeekday(), s.getStartTime(), s.getEndTime(), s.isActive(),
                        s.getCapacity(), s.getExpectedKg(), null))
                    .toList(),
            e.getStatus(),
            e.getLatitude(),
            e.getLongitude(),
            e.getParkingInfo(),
            e.getAccessInstructions(),
            e.getPickupProcedure(),
            e.getOnSiteContactNote(),
            e.getPreferredFoodCategories() == null
                ? List.of()
                : e.getPreferredFoodCategories().stream()
                    .map(PartnerEntity.PreferredFoodCategoryEmbeddable::getFoodCategoryId)
                    .toList()
        );
    }

    static void applyToEntity(PartnerEntity target, Partner src) {
        target.setName(src.name());
        target.setCategoryId(src.categoryId());
        target.setStreet(src.street());
        target.setPostalCode(src.postalCode());
        target.setCity(src.city());
        target.setLogoUrl(src.logoUrl());
        target.setStatus(src.status() != null ? src.status() : Partner.Status.KEIN_KONTAKT);
        target.setParkingInfo(src.parkingInfo());
        target.setAccessInstructions(src.accessInstructions());
        target.setPickupProcedure(src.pickupProcedure());
        target.setOnSiteContactNote(src.onSiteContactNote());

        PartnerEntity.ContactEmbeddable contact = target.getContact() != null
            ? target.getContact()
            : new PartnerEntity.ContactEmbeddable();
        if (src.contact() != null) {
            contact.setName(src.contact().name());
            contact.setEmail(src.contact().email());
            contact.setPhone(src.contact().phone());
        }
        target.setContact(contact);

        target.getPickupSlots().clear();
        if (src.pickupSlots() != null) {
            for (Partner.PickupSlot slot : src.pickupSlots()) {
                PartnerEntity.PickupSlotEmbeddable e = new PartnerEntity.PickupSlotEmbeddable();
                e.setWeekday(slot.weekday());
                e.setStartTime(slot.startTime());
                e.setEndTime(slot.endTime());
                e.setActive(slot.active());
                e.setCapacity(Math.max(0, slot.capacity()));
                e.setExpectedKg(slot.expectedKg() != null && slot.expectedKg().signum() >= 0
                    ? slot.expectedKg()
                    : null);
                target.getPickupSlots().add(e);
            }
        }

        target.getPreferredFoodCategories().clear();
        if (src.preferredFoodCategoryIds() != null) {
            for (Long fcId : src.preferredFoodCategoryIds()) {
                if (fcId == null) continue;
                PartnerEntity.PreferredFoodCategoryEmbeddable e = new PartnerEntity.PreferredFoodCategoryEmbeddable();
                e.setFoodCategoryId(fcId);
                target.getPreferredFoodCategories().add(e);
            }
        }
    }
}

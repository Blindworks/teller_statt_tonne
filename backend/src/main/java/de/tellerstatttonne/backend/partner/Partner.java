package de.tellerstatttonne.backend.partner;

import java.math.BigDecimal;
import java.util.List;

public record Partner(
    Long id,
    String name,
    Long categoryId,
    String street,
    String postalCode,
    String city,
    String logoUrl,
    Contact contact,
    List<PickupSlot> pickupSlots,
    Status status,
    Double latitude,
    Double longitude,
    String parkingInfo,
    String accessInstructions,
    String pickupProcedure,
    String onSiteContactNote,
    List<Long> preferredFoodCategoryIds
) {
    public enum Status {
        KEIN_KONTAKT,
        VERHANDLUNGEN_LAUFEN,
        WILL_NICHT_KOOPERIEREN,
        KOOPERIERT,
        KOOPERIERT_FOODSHARING,
        SPENDET_AN_TAFEL,
        EXISTIERT_NICHT_MEHR
    }

    public enum Weekday { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

    public record Contact(String name, String email, String phone) {}

    public record PickupSlot(
        Weekday weekday,
        String startTime,
        String endTime,
        boolean active,
        int capacity,
        BigDecimal expectedKg,
        Integer availableMemberCount
    ) {
        public PickupSlot(Weekday weekday, String startTime, String endTime, boolean active) {
            this(weekday, startTime, endTime, active, 1, null, null);
        }
        public PickupSlot(Weekday weekday, String startTime, String endTime, boolean active, int capacity) {
            this(weekday, startTime, endTime, active, capacity, null, null);
        }
        public PickupSlot(Weekday weekday, String startTime, String endTime, boolean active,
                          int capacity, BigDecimal expectedKg) {
            this(weekday, startTime, endTime, active, capacity, expectedKg, null);
        }
    }

    public Partner(
        Long id, String name, Long categoryId, String street, String postalCode, String city,
        String logoUrl, Contact contact, List<PickupSlot> pickupSlots, Status status,
        Double latitude, Double longitude
    ) {
        this(id, name, categoryId, street, postalCode, city, logoUrl, contact, pickupSlots,
            status, latitude, longitude, null, null, null, null, List.of());
    }

    public Partner withId(Long newId) {
        return new Partner(newId, name, categoryId, street, postalCode, city, logoUrl,
            contact, pickupSlots, status, latitude, longitude,
            parkingInfo, accessInstructions, pickupProcedure, onSiteContactNote, preferredFoodCategoryIds);
    }
}

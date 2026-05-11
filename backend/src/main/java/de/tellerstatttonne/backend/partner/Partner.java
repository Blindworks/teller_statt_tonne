package de.tellerstatttonne.backend.partner;

import java.math.BigDecimal;
import java.util.List;

public record Partner(
    Long id,
    String name,
    Category category,
    String street,
    String postalCode,
    String city,
    String logoUrl,
    Contact contact,
    List<PickupSlot> pickupSlots,
    Status status,
    Double latitude,
    Double longitude
) {
    public enum Category { BAKERY, SUPERMARKET, CAFE, RESTAURANT, BUTCHER }

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

    public Partner withId(Long newId) {
        return new Partner(newId, name, category, street, postalCode, city, logoUrl,
            contact, pickupSlots, status, latitude, longitude);
    }
}

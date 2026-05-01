package de.tellerstatttonne.backend.partner;

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
    public enum Category { BAKERY, SUPERMARKET, CAFE, RESTAURANT }

    public enum Status { ACTIVE, INACTIVE, DELETED }

    public enum Weekday { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

    public record Contact(String name, String email, String phone) {}

    public record PickupSlot(
        Weekday weekday,
        String startTime,
        String endTime,
        boolean active,
        int capacity,
        Integer availableMemberCount
    ) {
        public PickupSlot(Weekday weekday, String startTime, String endTime, boolean active) {
            this(weekday, startTime, endTime, active, 1, null);
        }
        public PickupSlot(Weekday weekday, String startTime, String endTime, boolean active, int capacity) {
            this(weekday, startTime, endTime, active, capacity, null);
        }
    }

    public Partner withId(Long newId) {
        return new Partner(newId, name, category, street, postalCode, city, logoUrl,
            contact, pickupSlots, status, latitude, longitude);
    }
}

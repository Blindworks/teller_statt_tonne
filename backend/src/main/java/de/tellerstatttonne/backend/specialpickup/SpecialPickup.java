package de.tellerstatttonne.backend.specialpickup;

import java.time.LocalDate;

public record SpecialPickup(
    Long id,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    String street,
    String postalCode,
    String city,
    Double latitude,
    Double longitude,
    String logoUrl,
    Contact contact
) {
    public record Contact(String name, String email, String phone) {}
}

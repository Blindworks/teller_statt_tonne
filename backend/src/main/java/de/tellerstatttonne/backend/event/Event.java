package de.tellerstatttonne.backend.event;

import java.time.LocalDate;

public record Event(
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

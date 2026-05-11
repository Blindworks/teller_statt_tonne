package de.tellerstatttonne.backend.pickup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Pickup(
    Long id,
    Long partnerId,
    String partnerName,
    Long partnerCategoryId,
    String partnerStreet,
    String partnerCity,
    String partnerLogoUrl,
    Long eventId,
    String eventName,
    String eventLogoUrl,
    LocalDate date,
    String startTime,
    String endTime,
    Status status,
    int capacity,
    List<Assignment> assignments,
    String notes,
    BigDecimal savedKg
) {
    public enum Status { SCHEDULED, COMPLETED, CANCELLED }

    public record Assignment(Long memberId, String memberName, String memberAvatarUrl) {}

    public Pickup withId(Long newId) {
        return new Pickup(newId, partnerId, partnerName, partnerCategoryId,
            partnerStreet, partnerCity, partnerLogoUrl,
            eventId, eventName, eventLogoUrl, date,
            startTime, endTime, status, capacity, assignments, notes, savedKg);
    }
}

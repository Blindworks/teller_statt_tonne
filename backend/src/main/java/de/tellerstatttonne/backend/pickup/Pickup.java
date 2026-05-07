package de.tellerstatttonne.backend.pickup;

import de.tellerstatttonne.backend.partner.Partner;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Pickup(
    Long id,
    Long partnerId,
    String partnerName,
    Partner.Category partnerCategory,
    String partnerStreet,
    String partnerCity,
    String partnerLogoUrl,
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
        return new Pickup(newId, partnerId, partnerName, partnerCategory,
            partnerStreet, partnerCity, partnerLogoUrl, date,
            startTime, endTime, status, capacity, assignments, notes, savedKg);
    }
}

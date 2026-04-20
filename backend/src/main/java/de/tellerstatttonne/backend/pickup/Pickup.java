package de.tellerstatttonne.backend.pickup;

import de.tellerstatttonne.backend.partner.Partner;
import java.time.LocalDate;
import java.util.List;

public record Pickup(
    String id,
    String partnerId,
    String partnerName,
    Partner.Category partnerCategory,
    LocalDate date,
    String startTime,
    String endTime,
    Status status,
    int capacity,
    List<Assignment> assignments,
    String notes
) {
    public enum Status { SCHEDULED, COMPLETED, CANCELLED }

    public record Assignment(String memberId, String memberName, String memberAvatarUrl) {}

    public Pickup withId(String newId) {
        return new Pickup(newId, partnerId, partnerName, partnerCategory, date,
            startTime, endTime, status, capacity, assignments, notes);
    }
}

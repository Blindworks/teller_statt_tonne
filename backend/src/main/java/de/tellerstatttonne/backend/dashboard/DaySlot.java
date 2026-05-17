package de.tellerstatttonne.backend.dashboard;

import de.tellerstatttonne.backend.pickup.Pickup;
import java.time.LocalDate;
import java.util.List;

public record DaySlot(
    Long pickupId,
    Long partnerId,
    String partnerName,
    Long partnerCategoryId,
    String partnerStreet,
    String partnerCity,
    String partnerLogoUrl,
    Long specialPickupId,
    String specialPickupName,
    String specialPickupLogoUrl,
    LocalDate date,
    String startTime,
    String endTime,
    int capacity,
    List<Pickup.Assignment> assignments,
    boolean isTemplate,
    boolean currentUserAssigned
) {}

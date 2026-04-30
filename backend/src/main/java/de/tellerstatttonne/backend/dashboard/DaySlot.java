package de.tellerstatttonne.backend.dashboard;

import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.pickup.Pickup;
import java.time.LocalDate;
import java.util.List;

public record DaySlot(
    Long pickupId,
    Long partnerId,
    String partnerName,
    Partner.Category partnerCategory,
    String partnerStreet,
    String partnerCity,
    String partnerLogoUrl,
    LocalDate date,
    String startTime,
    String endTime,
    int capacity,
    List<Pickup.Assignment> assignments,
    boolean isTemplate,
    boolean currentUserAssigned
) {}

package de.tellerstatttonne.backend.user.availability;

import de.tellerstatttonne.backend.partner.Partner;

public record UserAvailability(
    Long id,
    Long userId,
    Partner.Weekday weekday,
    String startTime,
    String endTime
) {}

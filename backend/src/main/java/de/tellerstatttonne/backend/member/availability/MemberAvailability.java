package de.tellerstatttonne.backend.member.availability;

import de.tellerstatttonne.backend.partner.Partner;

public record MemberAvailability(
    Long id,
    Long memberId,
    Partner.Weekday weekday,
    String startTime,
    String endTime
) {}

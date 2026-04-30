package de.tellerstatttonne.backend.member.availability;

import de.tellerstatttonne.backend.partner.Partner;

public record MemberAvailability(
    String id,
    String memberId,
    Partner.Weekday weekday,
    String startTime,
    String endTime
) {}

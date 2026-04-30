package de.tellerstatttonne.backend.partner;

import de.tellerstatttonne.backend.member.Member;
import de.tellerstatttonne.backend.member.MemberRole;
import java.math.BigDecimal;
import java.time.LocalDate;

public record StoreMember(
    String id,
    String firstName,
    String lastName,
    MemberRole role,
    String email,
    String city,
    String photoUrl,
    Member.OnlineStatus onlineStatus,
    Member.Status status,
    LocalDate lastPickupDate,
    BigDecimal totalSavedKg,
    long pickupCount
) {}

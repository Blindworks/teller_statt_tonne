package de.tellerstatttonne.backend.partner;

import de.tellerstatttonne.backend.user.Role;
import de.tellerstatttonne.backend.user.UserEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

public record StoreMember(
    Long id,
    String firstName,
    String lastName,
    Role role,
    String email,
    String city,
    String photoUrl,
    UserEntity.OnlineStatus onlineStatus,
    UserEntity.Status status,
    LocalDate lastPickupDate,
    BigDecimal totalSavedKg,
    long pickupCount
) {}

package de.tellerstatttonne.backend.partner;

import de.tellerstatttonne.backend.user.UserEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StoreMember(
    Long id,
    String firstName,
    String lastName,
    List<String> roles,
    String email,
    String city,
    String photoUrl,
    UserEntity.OnlineStatus onlineStatus,
    UserEntity.Status status,
    LocalDate lastPickupDate,
    BigDecimal totalSavedKg,
    long pickupCount
) {}

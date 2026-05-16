package de.tellerstatttonne.backend.rescuercard;

import java.time.Instant;
import java.time.LocalDate;

public record RescuerCardContext(
    Long userId,
    String firstName,
    String lastName,
    String photoUrl,
    Instant introductionCompletedAt,
    boolean hygieneValid,
    LocalDate hygieneExpiryDate,
    CurrentPickup currentPickup,
    Instant generatedAt
) {
    public record CurrentPickup(
        Long pickupId,
        Long partnerId,
        String partnerName,
        LocalDate date,
        String startTime,
        String endTime,
        boolean active
    ) {}
}

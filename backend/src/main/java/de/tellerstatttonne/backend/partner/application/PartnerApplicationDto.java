package de.tellerstatttonne.backend.partner.application;

import java.time.Instant;

public record PartnerApplicationDto(
    Long id,
    Long partnerId,
    String partnerName,
    String partnerStreet,
    String partnerPostalCode,
    String partnerCity,
    Long userId,
    String userFirstName,
    String userLastName,
    String userEmail,
    String userPhotoUrl,
    ApplicationStatus status,
    String message,
    String decisionReason,
    Long decidedByUserId,
    String decidedByDisplayName,
    Instant createdAt,
    Instant updatedAt,
    Instant decidedAt
) {}

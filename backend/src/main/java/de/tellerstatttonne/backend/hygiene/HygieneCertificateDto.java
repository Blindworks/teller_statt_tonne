package de.tellerstatttonne.backend.hygiene;

import java.time.Instant;
import java.time.LocalDate;

public record HygieneCertificateDto(
    Long id,
    Long userId,
    String userFirstName,
    String userLastName,
    String userEmail,
    String userPhotoUrl,
    String mimeType,
    String originalFilename,
    Long fileSizeBytes,
    LocalDate issuedDate,
    HygieneCertificateStatus status,
    String rejectionReason,
    Long decidedByUserId,
    String decidedByDisplayName,
    Instant decidedAt,
    Instant createdAt,
    Instant updatedAt
) {}

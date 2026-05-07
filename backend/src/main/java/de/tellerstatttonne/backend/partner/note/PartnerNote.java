package de.tellerstatttonne.backend.partner.note;

import java.time.Instant;

public record PartnerNote(
    Long id,
    Long partnerId,
    String body,
    Visibility visibility,
    Instant createdAt,
    Long authorUserId,
    String authorDisplayName,
    boolean deleted
) {}

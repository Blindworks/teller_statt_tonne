package de.tellerstatttonne.backend.notification;

import java.time.Instant;

public record Notification(
    Long id,
    NotificationType type,
    String title,
    String body,
    Long relatedPickupId,
    Long relatedPartnerId,
    Long actorUserId,
    Instant createdAt,
    Instant readAt
) {}
